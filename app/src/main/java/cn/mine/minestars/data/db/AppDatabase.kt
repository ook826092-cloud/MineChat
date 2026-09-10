package cn.mine.minestars.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.mine.ai.core.TokenUsage
import cn.mine.minestars.data.db.dao.ASRProviderDAO
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.AssistantMcpServerDAO
import cn.mine.minestars.data.db.dao.AssistantTagDAO
import cn.mine.minestars.data.db.dao.CharacterCardDAO
import cn.mine.minestars.data.db.dao.ConversationDAO
import cn.mine.minestars.data.db.dao.FolderDAO
import cn.mine.minestars.data.db.dao.FavoriteDAO
import cn.mine.minestars.data.db.dao.GenMediaDAO
import cn.mine.minestars.data.db.dao.KnowledgeBaseDAO
import cn.mine.minestars.data.db.dao.ManagedFileDAO
import cn.mine.minestars.data.db.dao.McpServerDAO
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.dao.MemoryDAO
import cn.mine.minestars.data.db.dao.MessageNodeDAO
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.dao.QuickMessageDAO
import cn.mine.minestars.data.db.dao.RegexGroupDAO
import cn.mine.minestars.data.db.dao.RegexScriptDAO
import cn.mine.minestars.data.db.dao.SearchServiceDAO
import cn.mine.minestars.data.db.dao.TTSProviderDAO
import cn.mine.minestars.data.db.dao.UserPersonaDAO
import cn.mine.minestars.data.db.dao.TagDAO
import cn.mine.minestars.data.db.dao.WorkspaceDAO
import cn.mine.minestars.data.db.dao.WorldBookDAO
import cn.mine.minestars.data.db.dao.WorldBookEntryDAO
import cn.mine.minestars.data.db.entity.ASRProviderEntity
import cn.mine.minestars.data.db.entity.AssistantEntity
import cn.mine.minestars.data.db.entity.AssistantMcpServerEntity
import cn.mine.minestars.data.db.entity.AssistantTagEntity
import cn.mine.minestars.data.db.entity.CharacterCardEntity
import cn.mine.minestars.data.db.entity.ConversationEntity
import cn.mine.minestars.data.db.entity.FolderEntity
import cn.mine.minestars.data.db.entity.FavoriteEntity
import cn.mine.minestars.data.db.entity.GenMediaEntity
import cn.mine.minestars.data.db.entity.KnowledgeBaseEntity
import cn.mine.minestars.data.db.entity.ManagedFileEntity
import cn.mine.minestars.data.db.entity.McpServerEntity
import cn.mine.minestars.data.db.entity.MemoryEntity
import cn.mine.minestars.data.db.entity.ModelSelectionEntity
import cn.mine.minestars.data.db.entity.MessageNodeEntity
import cn.mine.minestars.data.db.entity.PresetEntity
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.data.db.entity.ProviderEntity
import cn.mine.minestars.data.db.entity.QuickMessageEntity
import cn.mine.minestars.data.db.entity.RegexGroupEntity
import cn.mine.minestars.data.db.entity.RegexScriptEntity
import cn.mine.minestars.data.db.entity.SearchServiceEntity
import cn.mine.minestars.data.db.entity.TTSProviderEntity
import cn.mine.minestars.data.db.entity.TagEntity
import cn.mine.minestars.data.db.entity.UserPersonaEntity
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import cn.mine.minestars.data.db.entity.WorldBookEntity
import cn.mine.minestars.data.db.entity.WorldBookEntryEntity
import cn.mine.minestars.utils.JsonInstant

@Database(
    entities = [
        // ===== 对话/消息 =====
        ConversationEntity::class,
        MessageNodeEntity::class,
        // ===== 记忆 =====
        MemoryEntity::class,
        // ===== 生成媒体 =====
        GenMediaEntity::class,
        // ===== 文件 =====
        ManagedFileEntity::class,
        // ===== 收藏 =====
        FavoriteEntity::class,
        // ===== 助手及相关联表 =====
        AssistantEntity::class,
        AssistantTagEntity::class,
        AssistantMcpServerEntity::class,
        // ===== 角色卡 =====
        CharacterCardEntity::class,
        // ===== 预设 =====
        PresetEntity::class,
        PresetEntryEntity::class,
        // ===== 世界书 =====
        WorldBookEntity::class,
        WorldBookEntryEntity::class,
        // ===== 正则 =====
        RegexGroupEntity::class,
        RegexScriptEntity::class,
        // ===== 提供商 =====
        ProviderEntity::class,
        // ===== 模型选择 =====
        ModelSelectionEntity::class,
        // ===== MCP =====
        McpServerEntity::class,
        // ===== 知识库 =====
        KnowledgeBaseEntity::class,
        // ===== 标签 =====
        TagEntity::class,
        // ===== 快捷消息 =====
        QuickMessageEntity::class,
        // ===== TTS/ASR =====
        TTSProviderEntity::class,
        ASRProviderEntity::class,
        // ===== 搜索 =====
        SearchServiceEntity::class,
        // ===== 用户设定 =====
        UserPersonaEntity::class,
        // ===== 工作区 =====
        WorkspaceEntity::class,
        FolderEntity::class,
    ],
    version = 16,
)
@TypeConverters(TokenUsageConverter::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `model_selections` (
                    `id` INTEGER NOT NULL DEFAULT 1,
                    `chat_model_id` TEXT,
                    `fast_model_id` TEXT,
                    `title_model_id` TEXT,
                    `image_generation_model_id` TEXT,
                    `translate_model_id` TEXT,
                    `suggestion_model_id` TEXT,
                    `ocr_model_id` TEXT,
                    `compress_model_id` TEXT,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("INSERT OR IGNORE INTO model_selections (id) VALUES (1)")
        }

        val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN user_persona_id TEXT")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `user_personas` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }

        val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN regex_group_ids TEXT")
        }

        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN world_book_ids TEXT")
        }

        val MIGRATION_5_7 = Migration(5, 7) { db ->
            // Add JSON columns for assistant-level world book and regex scripts
            db.execSQL("ALTER TABLE assistants ADD COLUMN world_book_json TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE assistants ADD COLUMN regex_scripts_json TEXT DEFAULT NULL")
            // Drop dead table (was never used in any VM)
            db.execSQL("DROP TABLE IF EXISTS assistant_world_books")
            // Clean up orphaned tables from unapplied MIGRATION_5_6 (dev builds only)
            db.execSQL("DROP TABLE IF EXISTS assistant_regex_scripts")
            db.execSQL("DROP TABLE IF EXISTS assistant_regex_scripts_backup")
        }

        val MIGRATION_6_7 = Migration(6, 7) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN world_book_json TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE assistants ADD COLUMN regex_scripts_json TEXT DEFAULT NULL")
            db.execSQL("DROP TABLE IF EXISTS assistant_world_books")
            db.execSQL("DROP TABLE IF EXISTS assistant_regex_scripts")
            db.execSQL("DROP TABLE IF EXISTS assistant_regex_scripts_backup")
        }

        val MIGRATION_7_8 = Migration(7, 8) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN custom_headers TEXT DEFAULT '[]'")
            db.execSQL("ALTER TABLE assistants ADD COLUMN custom_bodies TEXT DEFAULT '[]'")
        }

        val MIGRATION_8_9 = Migration(8, 9) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `workspaces` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `root` TEXT NOT NULL,
                    `shell_enabled` INTEGER NOT NULL DEFAULT 0,
                    `shell_status` TEXT NOT NULL DEFAULT 'DISABLED',
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `last_access_at` INTEGER,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("ALTER TABLE assistants ADD COLUMN workspace_id TEXT DEFAULT NULL")
        }

        val MIGRATION_9_10 = Migration(9, 10) {
            // no-op：仅为保持升级链版本连续性
        }

        val MIGRATION_10_11 = Migration(10, 11) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN enable_image_gen INTEGER NOT NULL DEFAULT 0")
        }

        val MIGRATION_11_12 = Migration(11, 12) { db ->
            db.execSQL("ALTER TABLE assistants ADD COLUMN mcp_server_ids TEXT NOT NULL DEFAULT '[]'")
        }

        val MIGRATION_12_13 = Migration(12, 13) { db ->
            db.execSQL("ALTER TABLE workspaces DROP COLUMN shell_enabled")
        }

        val MIGRATION_13_14 = Migration(13, 14) {
            // no-op：仅为保持升级链版本连续性
        }

        val MIGRATION_14_15 = Migration(14, 15) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `conversation_folder` (
                    `id` TEXT NOT NULL,
                    `assistant_id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sort_index` INTEGER NOT NULL DEFAULT 0,
                    `create_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_folder_assistant_id` ON `conversation_folder` (`assistant_id`)")
            db.execSQL("ALTER TABLE `conversations` ADD COLUMN `folder_id` TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_15_16 = Migration(15, 16) { db ->
            db.execSQL("ALTER TABLE `assistants` DROP COLUMN `enable_image_gen`")
        }
    }

    abstract fun conversationDao(): ConversationDAO
    abstract fun memoryDao(): MemoryDAO
    abstract fun genMediaDao(): GenMediaDAO
    abstract fun messageNodeDao(): MessageNodeDAO
    abstract fun managedFileDao(): ManagedFileDAO
    abstract fun favoriteDao(): FavoriteDAO
    abstract fun assistantDao(): AssistantDAO
    abstract fun assistantTagDao(): AssistantTagDAO
    abstract fun assistantMcpServerDao(): AssistantMcpServerDAO
    abstract fun characterCardDao(): CharacterCardDAO
    abstract fun presetDao(): PresetDAO
    abstract fun presetEntryDao(): PresetEntryDAO
    abstract fun worldBookDao(): WorldBookDAO
    abstract fun worldBookEntryDao(): WorldBookEntryDAO
    abstract fun regexGroupDao(): RegexGroupDAO
    abstract fun regexScriptDao(): RegexScriptDAO
    abstract fun providerDao(): ProviderDAO
    abstract fun mcpServerDao(): McpServerDAO
    abstract fun modelSelectionDao(): ModelSelectionDAO
    abstract fun knowledgeBaseDao(): KnowledgeBaseDAO
    abstract fun tagDao(): TagDAO
    abstract fun quickMessageDao(): QuickMessageDAO
    abstract fun ttsProviderDao(): TTSProviderDAO
    abstract fun asrProviderDao(): ASRProviderDAO
    abstract fun searchServiceDao(): SearchServiceDAO
    abstract fun userPersonaDao(): UserPersonaDAO
    abstract fun workspaceDao(): WorkspaceDAO
    abstract fun folderDao(): FolderDAO
}

object TokenUsageConverter {
    @TypeConverter
    fun fromTokenUsage(usage: TokenUsage?): String {
        return JsonInstant.encodeToString(usage)
    }

    @TypeConverter
    fun toTokenUsage(usage: String): TokenUsage? {
        return JsonInstant.decodeFromString(usage)
    }
}
