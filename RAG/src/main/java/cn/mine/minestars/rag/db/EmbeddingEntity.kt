package cn.mine.minestars.rag.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "embeddings",
    indices = [
        Index("knowledge_base_id"),
        Index("knowledge_base_id", "file_uri"),
    ]
)
data class EmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "knowledge_base_id")
    val knowledgeBaseId: String,
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    @ColumnInfo(name = "chunk_index")
    val chunkIndex: Int,
    @ColumnInfo(name = "chunk_text")
    val chunkText: String,
    @ColumnInfo(name = "embedding")
    val embedding: String, // JSON serialized List<Float>
)
