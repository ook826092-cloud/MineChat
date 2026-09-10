package cn.mine.minestars.data.ai.mcp

import android.util.Log
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.StringValues
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import cn.mine.ai.core.InputSchema
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.AppScope
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.AssistantMcpServerDAO
import cn.mine.minestars.data.db.dao.McpServerDAO
import cn.mine.minestars.data.db.entity.McpServerEntity
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.data.files.saveUploadFromBytes
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.checkDifferent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val TAG = "McpManager"
private const val MAX_RECONNECT_ATTEMPTS = 5
private const val BASE_RECONNECT_DELAY_MS = 1000L
private const val MAX_RECONNECT_DELAY_MS = 30000L

class McpManager(
    private val mcpServerDao: McpServerDAO,
    private val assistantMcpServerDao: AssistantMcpServerDAO,
    private val assistantDao: AssistantDAO,
    private val settingsStore: SettingsStore,
    private val appScope: AppScope,
    private val filesManager: FilesManager,
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followSslRedirects(true)
        .followRedirects(true)
        .build()

    private val client = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(SSE)
    }

    private val clients: MutableMap<McpServerConfig, Client> = mutableMapOf()
    private val reconnectJobs: MutableMap<Uuid, Job> = mutableMapOf()
    private val reconnectAttempts: MutableMap<Uuid, Int> = mutableMapOf()
    val syncingStatus = MutableStateFlow<Map<Uuid, McpStatus>>(mapOf())

    init {
        appScope.launch {
            mcpServerDao.getAllFlow()
                .collect { entities ->
                    runCatching {
                        val configs = entities.map { it.toConfig() }
                        Log.i(TAG, "update configs: $configs")
                        val newConfigs = configs.filter { config ->
                            config.commonOptions.enable && when (config) {
                                is McpServerConfig.SseTransportServer ->
                                    config.url.startsWith("http://") || config.url.startsWith("https://")
                                is McpServerConfig.StreamableHTTPServer ->
                                    config.url.startsWith("http://") || config.url.startsWith("https://")
                            }
                        }
                        val currentConfigs = clients.keys.toList()
                        val (toAdd, toRemove) = currentConfigs.checkDifferent(
                            other = newConfigs,
                            eq = { a, b -> a.id == b.id }
                        )
                        Log.i(TAG, "to_add: $toAdd")
                        Log.i(TAG, "to_remove: $toRemove")
                        toAdd.forEach { cfg ->
                            appScope.launch {
                                runCatching { addClient(cfg) }
                                    .onFailure { it.printStackTrace() }
                            }
                        }
                        toRemove.forEach { cfg ->
                            appScope.launch { removeClient(cfg) }
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
        }
    }

    fun getClient(config: McpServerConfig): Client? {
        return clients.entries.find { it.key.id == config.id }?.value
    }

    suspend fun getAllAvailableTools(): List<Pair<Uuid, McpTool>> {
        val assistantId = settingsStore.settingsFlow.value.assistantId
        val assistant = assistantDao.getById(assistantId.toString())
        val linkedServerIds = if (assistant != null) {
            assistantMcpServerDao.getServersOfAssistant(assistant.id).map { it.mcpServerId }.toSet()
        } else emptySet()
        val allServers = mcpServerDao.getEnabled()
        return allServers
            .filter { it.id in linkedServerIds }
            .flatMap { entity ->
                val config = entity.toConfig()
                config.commonOptions.tools
                    .filter { tool -> tool.enable }
                    .map { tool -> entity.id.toUuid() to tool }
            }
    }

    suspend fun callTool(serverId: Uuid, toolName: String, args: JsonObject): List<UIMessagePart> {
        val entry = clients.entries.find { it.key.id == serverId }
        val client = entry?.value
            ?: return listOf(UIMessagePart.Text("Failed to execute tool, because no such mcp client for the tool"))
        val config = entry.key
        Log.i(TAG, "callTool: $toolName / $args (server: ${config.commonOptions.name})")

        if (client.transport == null) client.connect(getTransport(config))
        val result = client.callTool(
            request = CallToolRequest(
                params = CallToolRequestParams(
                    name = toolName,
                    arguments = args,
                ),
            ),
            options = RequestOptions(timeout = 120.seconds),
        )
        return result.content.map {
            when(it) {
                is TextContent -> UIMessagePart.Text(it.text)
                is ImageContent -> convertImageContentToFilePart(it)
                else -> UIMessagePart.Text(JsonInstant.encodeToString(it))
            }
        }
    }

    private suspend fun convertImageContentToFilePart(image: ImageContent): UIMessagePart.Image {
        val bytes = Base64.decode(image.data)
        val ext = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(image.mimeType) ?: "bin"
        val entity = filesManager.saveUploadFromBytes(
            bytes = bytes,
            displayName = "mcp_image.$ext",
            mimeType = image.mimeType,
        )
        val uri = filesManager.getFile(entity).toUri()
        Log.i(TAG, "convertImageContentToFilePart: saved mcp image to $uri")
        return UIMessagePart.Image(url = uri.toString())
    }

    private fun getTransport(config: McpServerConfig): AbstractTransport = when (config) {
        is McpServerConfig.SseTransportServer -> {
            val url = config.url
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                throw IllegalArgumentException("Invalid SSE URL: $url")
            SseClientTransport(
                urlString = url,
                client = client,
                requestBuilder = {
                    headers.appendAll(StringValues.build {
                        config.commonOptions.headers.forEach {
                            append(it.first, it.second)
                        }
                    })
                },
            )
        }

        is McpServerConfig.StreamableHTTPServer -> {
            val url = config.url
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                throw IllegalArgumentException("Invalid HTTP URL: $url")
            StreamableHttpClientTransport(
                url = url,
                client = client,
                requestBuilder = {
                    headers.appendAll(StringValues.build {
                        config.commonOptions.headers.forEach {
                            append(it.first, it.second)
                        }
                    })
                }
            )
        }
    }

    suspend fun addClient(config: McpServerConfig) = withContext(Dispatchers.IO) {
        removeClient(config)
        cancelReconnect(config.id)
        reconnectAttempts[config.id] = 0

        val transport = getTransport(config)
        val client = Client(
            clientInfo = Implementation(
                name = config.commonOptions.name,
                version = "1.0",
            )
        )

        transport.onClose {
            Log.i(TAG, "Transport closed for ${config.commonOptions.name}")
            val currentStatus = syncingStatus.value[config.id]
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        transport.onError { error ->
            Log.e(TAG, "Transport error for ${config.commonOptions.name}: ${error.message}")
            val currentStatus = syncingStatus.value[config.id]
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        clients[config] = client
        runCatching {
            setStatus(config = config, status = McpStatus.Connecting)
            client.connect(transport)
            sync(config)
            setStatus(config = config, status = McpStatus.Connected)
            reconnectAttempts[config.id] = 0
            Log.i(TAG, "addClient: connected ${config.commonOptions.name}")
        }.onFailure {
            it.printStackTrace()
            setStatus(config = config, status = McpStatus.Error(it.message ?: it.javaClass.name))
        }
    }

    private suspend fun sync(config: McpServerConfig) {
        val client = clients[config] ?: return

        setStatus(config = config, status = McpStatus.Connecting)

        if (client.transport == null) {
            client.connect(getTransport(config))
        }
        val serverTools = client.listTools().tools
        Log.i(TAG, "sync: tools: $serverTools")

        // 从数据库读取当前实体，更新 tools 后写回
        val entity = mcpServerDao.getById(config.id.toString()) ?: return
        val currentConfig = entity.toConfig()
        val common = currentConfig.commonOptions
        val tools = common.tools.toMutableList()

        serverTools.forEach { serverTool ->
            val tool = tools.find { it.name == serverTool.name }
            if (tool == null) {
                tools.add(
                    McpTool(
                        name = serverTool.name,
                        description = serverTool.description,
                        enable = true,
                        inputSchema = serverTool.inputSchema.toSchema()
                    )
                )
            } else {
                val index = tools.indexOf(tool)
                tools[index] = tool.copy(
                    description = serverTool.description,
                    inputSchema = serverTool.inputSchema.toSchema()
                )
            }
        }
        tools.removeIf { tool -> serverTools.none { it.name == tool.name } }

        val updatedConfig = currentConfig.clone(
            commonOptions = common.copy(tools = tools)
        )

        // 更新 clients map
        clients.remove(config)
        clients[updatedConfig] = client

        // 写回数据库
        mcpServerDao.insert(updatedConfig.toEntity())

        setStatus(config = config, status = McpStatus.Connected)
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        clients.keys.toList().forEach { config ->
            runCatching {
                sync(config)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    suspend fun removeClient(config: McpServerConfig) = withContext(Dispatchers.IO) {
        cancelReconnect(config.id)
        val toRemove = clients.entries.filter { it.key.id == config.id }
        toRemove.forEach { entry ->
            runCatching {
                entry.value.close()
            }.onFailure {
                it.printStackTrace()
            }
            clients.remove(entry.key)
            syncingStatus.emit(syncingStatus.value.toMutableMap().apply { remove(entry.key.id) })
            Log.i(TAG, "removeClient: ${entry.key} / ${entry.key.commonOptions.name}")
        }
        reconnectAttempts.remove(config.id)
    }

    private fun scheduleReconnect(config: McpServerConfig) {
        val configId = config.id
        val currentAttempt = (reconnectAttempts[configId] ?: 0) + 1

        if (currentAttempt > MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached for ${config.commonOptions.name}")
            appScope.launch {
                setStatus(config, McpStatus.Error("连接断开，已达最大重连次数"))
            }
            return
        }

        reconnectAttempts[configId] = currentAttempt
        reconnectJobs[configId]?.cancel()

        val delayMs = calculateBackoffDelay(currentAttempt)
        Log.i(TAG, "Scheduling reconnect for ${config.commonOptions.name}, attempt $currentAttempt/$MAX_RECONNECT_ATTEMPTS, delay ${delayMs}ms")

        reconnectJobs[configId] = appScope.launch {
            try {
                setStatus(config, McpStatus.Reconnecting(currentAttempt, MAX_RECONNECT_ATTEMPTS))
                delay(delayMs)

                val entity = mcpServerDao.getById(configId.toString())
                if (entity == null || !entity.enabled) {
                    Log.i(TAG, "Config disabled or removed, cancelling reconnect for ${config.commonOptions.name}")
                    return@launch
                }

                Log.i(TAG, "Attempting reconnect for ${config.commonOptions.name}")
                reconnectClient(config)
            } catch (e: CancellationException) {
                Log.i(TAG, "Reconnect cancelled for ${config.commonOptions.name}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed for ${config.commonOptions.name}", e)
                scheduleReconnect(config)
            }
        }
    }

    private fun cancelReconnect(configId: Uuid) {
        reconnectJobs[configId]?.cancel()
        reconnectJobs.remove(configId)
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = BASE_RECONNECT_DELAY_MS * (1L shl (attempt - 1).coerceAtMost(10))
        return exponentialDelay.coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }

    private suspend fun reconnectClient(config: McpServerConfig) = withContext(Dispatchers.IO) {
        val oldEntry = clients.entries.find { it.key.id == config.id }
        if (oldEntry != null) {
            runCatching { oldEntry.value.close() }.onFailure { it.printStackTrace() }
            clients.remove(oldEntry.key)
        }

        val transport = getTransport(config)
        val client = Client(
            clientInfo = Implementation(
                name = config.commonOptions.name,
                version = "1.0",
            )
        )

        transport.onClose {
            Log.i(TAG, "Transport closed for ${config.commonOptions.name}")
            val currentStatus = syncingStatus.value[config.id]
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        transport.onError { error ->
            Log.e(TAG, "Transport error for ${config.commonOptions.name}: ${error.message}")
            val currentStatus = syncingStatus.value[config.id]
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        clients[config] = client
        setStatus(config, McpStatus.Connecting)
        client.connect(transport)
        sync(config)
        setStatus(config, McpStatus.Connected)
        reconnectAttempts[config.id] = 0
        Log.i(TAG, "Reconnected successfully: ${config.commonOptions.name}")
    }

    private suspend fun setStatus(config: McpServerConfig, status: McpStatus) {
        syncingStatus.emit(syncingStatus.value.toMutableMap().apply {
            put(config.id, status)
        })
    }

    fun getStatus(config: McpServerConfig): Flow<McpStatus> {
        return syncingStatus.map { it[config.id] ?: McpStatus.Idle }
    }
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal val McpJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        classDiscriminatorMode = ClassDiscriminatorMode.NONE
        explicitNulls = false
    }
}

private fun ToolSchema.toSchema(): InputSchema {
    return InputSchema.Obj(properties = this.properties ?: JsonObject(emptyMap()), required = this.required)
}

// ── McpServerConfig ↔ McpServerEntity 转换 ──

fun McpServerEntity.toConfig(): McpServerConfig {
    return JsonInstant.decodeFromString(this.config)
}

fun McpServerConfig.toEntity(): McpServerEntity {
    val configJson = JsonInstant.encodeToString(this)
    return McpServerEntity(
        id = this.id.toString(),
        name = this.commonOptions.name,
        type = when (this) {
            is McpServerConfig.SseTransportServer -> "sse"
            is McpServerConfig.StreamableHTTPServer -> "streamable_http"
        },
        enabled = this.commonOptions.enable,
        config = configJson,
        displayOrder = 0,
    )
}

private fun String.toUuid(): Uuid = Uuid.parse(this)
