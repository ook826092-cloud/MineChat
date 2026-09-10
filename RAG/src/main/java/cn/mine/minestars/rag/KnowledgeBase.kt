package cn.mine.minestars.rag

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * 知识库
 *
 * 每个知识库包含一组文件，通过嵌入模型索引后可用于语义检索。
 */
@Serializable
data class KnowledgeBase(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val embeddingModelId: Uuid? = null,
    val rerankModelId: Uuid? = null,
    val files: List<String> = emptyList(),
    val topK: Int = 5,
    val similarityThreshold: Float = 0.5f,
    val embeddingDimensions: Int? = null,
)
