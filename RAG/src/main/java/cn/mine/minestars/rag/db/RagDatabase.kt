package cn.mine.minestars.rag.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EmbeddingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RagDatabase : RoomDatabase() {
    abstract fun embeddingDAO(): EmbeddingDAO
}
