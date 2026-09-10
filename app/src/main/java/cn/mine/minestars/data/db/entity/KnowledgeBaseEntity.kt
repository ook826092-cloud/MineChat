package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_bases")
data class KnowledgeBaseEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "embedding_model_id") val embeddingModelId: String?,
    @ColumnInfo(name = "rerank_model_id") val rerankModelId: String?,
    @ColumnInfo(name = "top_k") val topK: Int,
    @ColumnInfo(name = "similarity_threshold") val similarityThreshold: Float,
    @ColumnInfo(name = "embedding_dimensions") val embeddingDimensions: Int?,
    val config: String,
)
