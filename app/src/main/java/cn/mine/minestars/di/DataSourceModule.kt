package cn.mine.minestars.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.HttpHeaders
import io.pebbletemplates.pebble.PebbleEngine
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import io.requery.android.database.sqlite.SQLiteCustomExtension
import kotlinx.serialization.json.Json
import cn.mine.ai.provider.ProviderManager
import cn.mine.common.http.AcceptLanguageBuilder
import cn.mine.minestars.BuildConfig
import cn.mine.minestars.data.ai.AIRequestInterceptor
import cn.mine.minestars.data.ai.RequestLoggingInterceptor
import cn.mine.minestars.data.ai.transformers.AssistantTemplateLoader
import cn.mine.minestars.data.ai.GenerationHandler
import cn.mine.minestars.data.ai.transformers.TemplateTransformer
import cn.mine.minestars.data.tavern.pipeline.TavernDataLoader
import cn.mine.minestars.data.api.MineChatAPI
import cn.mine.minestars.data.api.SponsorAPI
import cn.mine.minestars.data.datastore.AiSettingsStore
import cn.mine.minestars.data.datastore.DEFAULT_ASSISTANT_ID
import cn.mine.minestars.data.datastore.DEFAULT_PROVIDERS
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.core.tavern.preset.DefaultPresetFactory
import cn.mine.minestars.core.tavern.preset.PresetParser
import cn.mine.minestars.data.db.toEntity
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.db.AppDatabase
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.rag.EmbeddingService
import cn.mine.minestars.rag.ModelResolver
import cn.mine.minestars.rag.db.RagDatabase
import cn.mine.minestars.data.db.fts.MessageFtsManager
import cn.mine.minestars.data.sync.LocalBackupSync
import cn.mine.minestars.data.db.fts.SimpleDictManager
import cn.mine.minestars.data.ai.mcp.McpManager
import cn.mine.search.SearchService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

private fun createMessageFts(db: SupportSQLiteDatabase, context: Context) {
    val dictDir = SimpleDictManager.extractDict(context)
    val cursor = db.query("SELECT jieba_dict(?)", arrayOf(dictDir.absolutePath))
    cursor.use {
        if (it.moveToFirst()) {
            val result = it.getString(0)
            val success = result?.trimEnd('/') == dictDir.absolutePath.trimEnd('/')
            if (!success) {
                android.util.Log.e(
                    "DataSourceModule",
                    "jieba_dict failed: $result, path=${dictDir.absolutePath}"
                )
            }
        }
    }
    db.execSQL(
        """
        CREATE VIRTUAL TABLE IF NOT EXISTS message_fts USING fts5(
            text,
            node_id UNINDEXED,
            message_id UNINDEXED,
            conversation_id UNINDEXED,
            title UNINDEXED,
            update_at UNINDEXED,
            tokenize = 'simple'
        )
        """.trimIndent()
    )
}

private fun seedDefaultAssistant(db: SupportSQLiteDatabase) {
    val cursor = db.query("SELECT COUNT(*) FROM assistants")
    cursor.use {
        it.moveToFirst()
        if (it.getInt(0) > 0) return
    }

    // Seed default preset first (so the assistant can reference it)
    val presetId = DefaultPresetFactory.DEFAULT_PRESET_ID
    val presetJson = DefaultPresetFactory.createDefaultPresetJson()
    db.execSQL(
        """INSERT OR IGNORE INTO presets (id, name, raw_json, is_default) VALUES (?, ?, ?, 1)""",
        arrayOf<Any?>(presetId, "默认预设", presetJson)
    )

    // Seed default preset entries
    val presetObj = org.json.JSONObject(presetJson)
    val entries = PresetParser.extractEntries(presetObj)
    for ((index, entry) in entries.withIndex()) {
        db.execSQL(
            """INSERT OR IGNORE INTO preset_entries
               (preset_id, entry_index, id, identifier, name, enabled, role, content,
                injection_position, injection_depth, injection_order, system_prompt,
                marker, forbid_overrides, injection_trigger_json, mounted)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            arrayOf<Any?>(
                presetId, index, entry.id, entry.identifier, entry.name,
                if (entry.enabled) 1 else 0,
                entry.role, entry.content,
                entry.injectionPosition, entry.injectionDepth, entry.injectionOrder,
                if (entry.systemPrompt) 1 else 0,
                if (entry.marker) 1 else 0,
                if (entry.forbidOverrides) 1 else 0,
                entry.injectionTrigger.joinToString(","),
                if (entry.mounted) 1 else 0
            )
        )
    }

    // Seed default assistant with preset_id pointing to the default preset
    val entity = Assistant(id = DEFAULT_ASSISTANT_ID, presetId = kotlin.uuid.Uuid.parse(presetId)).toEntity()
    val values = android.content.ContentValues().apply {
        put("id", entity.id)
        put("name", entity.name)
        put("chat_model_id", entity.chatModelId)
        put("character_card_id", entity.characterCardId)
        put("knowledge_base_id", entity.knowledgeBaseId)
        put("preset_id", entity.presetId)
        put("avatar", entity.avatar)
        put("use_assistant_avatar", entity.useAssistantAvatar)
        put("enable_memory", entity.enableMemory)
        put("use_global_memory", entity.useGlobalMemory)
        put("enable_recent_chats_reference", entity.enableRecentChatsReference)
        put("message_template", entity.messageTemplate)
        put("quick_message_ids", entity.quickMessageIds)
        put("local_tools", entity.localTools)
        put("background", entity.background)
        put("background_opacity", entity.backgroundOpacity)
        put("enabled_skills", entity.enabledSkills)
        put("enable_time_reminder", entity.enableTimeReminder)
        put("allow_conversation_system_prompt", entity.allowConversationSystemPrompt)
        put("interleaved_thinking", entity.interleavedThinking)
        put("context_message_count", entity.contextMessageCount)
        put("char_name", entity.charName)
        put("description", entity.description)
        put("personality", entity.personality)
        put("scenario", entity.scenario)
        put("first_message", entity.firstMessage)
        put("mes_examples", entity.mesExamples)
        put("main_prompt_override", entity.mainPromptOverride)
        put("post_history_instructions", entity.postHistoryInstructions)
        put("creator_notes", entity.creatorNotes)
        put("alternate_greetings", entity.alternateGreetings)
        put("group_only_greetings", entity.groupOnlyGreetings)
        put("nickname", entity.nickname)
        put("card_tags", entity.cardTags)
        put("creator", entity.creator)
        put("character_version", entity.characterVersion)
        put("source", entity.source)
        put("display_order", entity.displayOrder)
        put("created_at", entity.createdAt)
        put("updated_at", entity.updatedAt)
        put("custom_headers", entity.customHeaders)
        put("custom_bodies", entity.customBodies)
        put("mcp_server_ids", entity.mcpServerIds)
    }
    db.insert("assistants", android.database.sqlite.SQLiteDatabase.CONFLICT_NONE, values)
}

private fun seedDefaultProviders(db: SupportSQLiteDatabase) {
    val cursor = db.query("SELECT COUNT(*) FROM providers")
    cursor.use {
        it.moveToFirst()
        if (it.getInt(0) > 0) return
    }
    val seedNames = setOf("OpenAI", "DeepSeek", "OpenCode")
    for ((index, provider) in DEFAULT_PROVIDERS.withIndex()) {
        if (provider.name !in seedNames) continue
        db.execSQL(
            """INSERT OR IGNORE INTO providers (id, name, type, enabled, built_in, config, display_order)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            arrayOf<Any?>(
                provider.id.toString(),
                provider.name,
                "openai",
                if (provider.enabled) 1 else 0,
                if (provider.builtIn) 1 else 0,
                JsonInstant.encodeToString(provider),
                index
            )
        )
    }
}

private fun seedModelSelections(db: SupportSQLiteDatabase) {
    val cursor = db.query("SELECT COUNT(*) FROM model_selections")
    cursor.use {
        it.moveToFirst()
        if (it.getInt(0) > 0) return
    }
    db.execSQL(
        """INSERT OR IGNORE INTO model_selections (id, chat_model_id) VALUES (1, ?)""",
        arrayOf<Any?>(null)
    )
}

val dataSourceModule = module {
    single {
        SettingsStore(context = get(), scope = get())
    }
    single {
        AiSettingsStore(context = get(), scope = get())
    }

    // ── CoreDatabase (minechat.db) ──
    single {
        val context: Context = get()
        Room.databaseBuilder(context, AppDatabase::class.java, "minechat")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_7, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createMessageFts(db, context)
                    seedDefaultProviders(db)
                    seedDefaultAssistant(db)
                    seedModelSelections(db)
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    createMessageFts(db, context)
                    seedDefaultProviders(db)
                    seedDefaultAssistant(db)
                    seedModelSelections(db)
                }
            })
            .openHelperFactory(
                RequerySQLiteOpenHelperFactory(
                    listOf(
                        RequerySQLiteOpenHelperFactory.ConfigurationOptions { options ->
                            options.customExtensions.add(
                                SQLiteCustomExtension(
                                    context.applicationInfo.nativeLibraryDir + "/libsimple",
                                    null
                                )
                            )
                            options
                        }
                    )
                )
            )
            .build()
    }

    single {
        AssistantTemplateLoader(settingsStore = get(), assistantDAO = get())
    }

    single {
        PebbleEngine.Builder()
            .loader(get<AssistantTemplateLoader>())
            .defaultLocale(Locale.getDefault())
            .autoEscaping(false)
            .build()
    }

    single { TemplateTransformer(engine = get(), settingsStore = get()) }

    // ── DAOs ──
    single { get<AppDatabase>().conversationDao() }
    single { get<AppDatabase>().memoryDao() }
    single { get<AppDatabase>().genMediaDao() }
    single { get<AppDatabase>().messageNodeDao() }
    single { get<AppDatabase>().managedFileDao() }
    single { get<AppDatabase>().favoriteDao() }
    single { get<AppDatabase>().assistantDao() }
    single { get<AppDatabase>().assistantTagDao() }
    single { get<AppDatabase>().assistantMcpServerDao() }
    single { get<AppDatabase>().characterCardDao() }
    single { get<AppDatabase>().presetDao() }
    single { get<AppDatabase>().presetEntryDao() }
    single { get<AppDatabase>().worldBookDao() }
    single { get<AppDatabase>().worldBookEntryDao() }
    single { get<AppDatabase>().regexGroupDao() }
    single { get<AppDatabase>().regexScriptDao() }
    single { get<AppDatabase>().providerDao() }
    single { get<AppDatabase>().modelSelectionDao() }
    single { get<AppDatabase>().mcpServerDao() }
    single { get<AppDatabase>().knowledgeBaseDao() }
    single { get<AppDatabase>().tagDao() }
    single { get<AppDatabase>().quickMessageDao() }
    single { get<AppDatabase>().ttsProviderDao() }
    single { get<AppDatabase>().asrProviderDao() }
    single { get<AppDatabase>().searchServiceDao() }
    single { get<AppDatabase>().userPersonaDao() }
    single { get<AppDatabase>().workspaceDao() }
    single { get<AppDatabase>().folderDao() }

    single { MessageFtsManager(get()) }

    // ── Tavern services ──
    single {
        TavernDataLoader(
            presetDao = get(),
            presetEntryDao = get(),
            worldBookDao = get(),
            regexScriptDao = get(),
            regexGroupDao = get(),
        )
    }

    single {
        McpManager(
            mcpServerDao = get(),
            assistantMcpServerDao = get(),
            assistantDao = get(),
            settingsStore = get(),
            appScope = get(),
            filesManager = get(),
        )
    }

    single {
        GenerationHandler(
            context = get(),
            providerManager = get(),
            json = get(),
            memoryRepo = get(),
            conversationRepo = get(),
            aiLoggingManager = get(),
            tavernDataLoader = get(),
            providerDao = get(),
            modelSelectionDAO = get(),
            userPersonaDao = get(),
            aiSettingsStore = get(),
        )
    }

    // ── HTTP / Network ──
    single<OkHttpClient> {
        val acceptLang = AcceptLanguageBuilder.fromAndroid(get()).build()
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .addHeader(HttpHeaders.AcceptLanguage, acceptLang)
                if (originalRequest.header(HttpHeaders.UserAgent) == null) {
                    requestBuilder.addHeader(HttpHeaders.UserAgent, "MineChat-Android/${BuildConfig.VERSION_NAME}")
                }
                chain.proceed(requestBuilder.build())
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val contentTypeHeader = request.header("Content-Type")
                if (
                    contentTypeHeader != null &&
                    contentTypeHeader.contains(";") &&
                    contentTypeHeader.substringBefore(";").trim().equals("application/json", ignoreCase = true)
                ) {
                    chain.proceed(
                        request.newBuilder()
                            .header("Content-Type", contentTypeHeader.substringBefore(";").trim())
                            .build()
                    )
                } else {
                    chain.proceed(request)
                }
            }
            .addInterceptor(RequestLoggingInterceptor(settingsStore = get()))
            .addInterceptor(AIRequestInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build().also { SearchService.init(it, get()) }
    }

    single { SponsorAPI.create(get()) }
    single { ProviderManager(client = get(), context = get()) }

    single {
        LocalBackupSync(
            context = get(),
            settingsStore = get(),
            appDatabase = get(),
        )
    }

    single<HttpClient> {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(20, TimeUnit.SECONDS)
                    readTimeout(10, TimeUnit.MINUTES)
                    writeTimeout(120, TimeUnit.SECONDS)
                    followSslRedirects(true)
                    followRedirects(true)
                    retryOnConnectionFailure(true)
                }
            }
        }
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://api.rikka-ai.com")
            .addConverterFactory(get<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    single<MineChatAPI> {
        get<Retrofit>().create(MineChatAPI::class.java)
    }

    // ── RAG / Knowledge Base ──
    single {
        Room.databaseBuilder(get(), RagDatabase::class.java, "rag_db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    single { get<RagDatabase>().embeddingDAO() }

    single {
        EmbeddingService(
            context = get(),
            embeddingDao = get(),
            providerManager = get(),
            json = get(),
            modelResolver = ModelResolver { embeddingModelId ->
                kotlinx.coroutines.runBlocking {
                    val providers = get<cn.mine.minestars.data.db.dao.ProviderDAO>().getAll()
                    val model = providers.findModelById(embeddingModelId) ?: return@runBlocking null
                    val provider = model.findProviderFromList(providers) ?: return@runBlocking null
                    model to provider
                }
            },
        )
    }
}

private fun List<cn.mine.minestars.data.db.entity.ProviderEntity>.toProviderSettings(): List<cn.mine.ai.provider.ProviderSetting> {
    return map { entity ->
        cn.mine.minestars.utils.JsonInstant.decodeFromString(entity.config)
    }
}

private fun List<cn.mine.minestars.data.db.entity.ProviderEntity>.findModelById(uuid: kotlin.uuid.Uuid): cn.mine.ai.provider.Model? {
    val settings = toProviderSettings()
    settings.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == uuid) return model
        }
    }
    return null
}

private fun cn.mine.ai.provider.Model.findProviderFromList(
    entities: List<cn.mine.minestars.data.db.entity.ProviderEntity>
): cn.mine.ai.provider.ProviderSetting? {
    val settings = entities.toProviderSettings()
    val provider = findModelProviderFromList(settings) ?: return null
    val providerOverwrite = this.providerOverwrite
    if (providerOverwrite != null) {
        return providerOverwrite.copyProvider(models = emptyList())
    }
    return provider
}

private fun cn.mine.ai.provider.Model.findModelProviderFromList(
    providers: List<cn.mine.ai.provider.ProviderSetting>
): cn.mine.ai.provider.ProviderSetting? {
    providers.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == this.id) return setting
        }
    }
    return null
}
