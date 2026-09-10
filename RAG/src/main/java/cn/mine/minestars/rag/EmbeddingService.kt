package cn.mine.minestars.rag

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import cn.mine.ai.provider.EmbeddingGenerationParams
import kotlin.uuid.Uuid
import cn.mine.ai.provider.Model
import cn.mine.ai.provider.ProviderManager
import cn.mine.ai.provider.ProviderSetting
import cn.mine.minestars.rag.db.EmbeddingDAO
import cn.mine.minestars.rag.db.EmbeddingEntity
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sqrt

private const val TAG = "EmbeddingService"

/**
 * 模型解析接口，由 app 模块实现
 */
fun interface ModelResolver {
    fun resolve(embeddingModelId: Uuid): Pair<Model, ProviderSetting>?
}

/**
 * 核心向量化服务
 */
class EmbeddingService(
    private val context: Context,
    private val embeddingDao: EmbeddingDAO,
    private val providerManager: ProviderManager,
    private val json: Json,
    private val modelResolver: ModelResolver,
) {
    private val indexingLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * 对文件进行向量化索引
     */
    suspend fun embedFile(
        kb: KnowledgeBase,
        fileUri: String,
        model: Model? = null,
        providerSetting: ProviderSetting? = null,
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting embedding for file: $fileUri in KB: ${kb.name}")

        val (resolvedModel, resolvedProvider) = resolveModelAndProvider(kb, model, providerSetting)
            ?: throw IllegalStateException("未配置嵌入模型，请在配置 Tab 中选择一个嵌入模型")

        val content = readFileContent(fileUri) ?: run {
            Log.w(TAG, "Failed to read file: $fileUri")
            throw IllegalArgumentException("无法读取文件内容，可能是权限丢失或文件已损坏。")
        }

        val chunks = TextChunker.chunk(content)
        if (chunks.isEmpty()) {
            Log.w(TAG, "No chunks produced from file: $fileUri")
            return@withContext
        }
        Log.i(TAG, "File chunked into ${chunks.size} pieces")

        embeddingDao.deleteByFileUri(kb.id.toString(), fileUri)

        val batchSize = 20
        val allEntities = mutableListOf<EmbeddingEntity>()

        for (batchStart in chunks.indices step batchSize) {
            val batchEnd = minOf(batchStart + batchSize, chunks.size)
            val batchChunks = chunks.subList(batchStart, batchEnd)

            try {
                val provider = providerManager.getProviderByType(resolvedProvider)
                val result = provider.generateEmbedding(
                    providerSetting = resolvedProvider,
                    params = EmbeddingGenerationParams(
                        model = resolvedModel,
                        input = batchChunks,
                        dimensions = kb.embeddingDimensions,
                    ),
                )

                result.embeddings.forEachIndexed { i, embedding ->
                    allEntities.add(
                        EmbeddingEntity(
                            knowledgeBaseId = kb.id.toString(),
                            fileUri = fileUri,
                            chunkIndex = batchStart + i,
                            chunkText = batchChunks[i],
                            embedding = json.encodeToString(
                                ListSerializer(Float.serializer()),
                                embedding
                            ),
                        )
                    )
                }
                Log.i(TAG, "Batch ${batchStart / batchSize + 1} embedded: ${batchChunks.size} chunks")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to embed batch starting at $batchStart", e)
                throw e
            }
        }

        embeddingDao.insertAll(allEntities)
        Log.i(TAG, "Successfully embedded ${allEntities.size} chunks for file: $fileUri")
    }

    suspend fun removeFile(kbId: String, fileUri: String) = withContext(Dispatchers.IO) {
        embeddingDao.deleteByFileUri(kbId, fileUri)
        Log.i(TAG, "Removed embeddings for file: $fileUri")
    }

    suspend fun removeKnowledgeBase(kbId: String) = withContext(Dispatchers.IO) {
        embeddingDao.deleteByKnowledgeBaseId(kbId)
        Log.i(TAG, "Removed all embeddings for KB: $kbId")
    }

    suspend fun searchSimilar(
        kb: KnowledgeBase,
        query: String,
        topK: Int = kb.topK,
        threshold: Float = kb.similarityThreshold,
        model: Model? = null,
        providerSetting: ProviderSetting? = null,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val (resolvedModel, resolvedProvider) = resolveModelAndProvider(kb, model, providerSetting)
            ?: return@withContext emptyList()

        val queryEmbedding = try {
            val provider = providerManager.getProviderByType(resolvedProvider)
            val result = provider.generateEmbedding(
                providerSetting = resolvedProvider,
                params = EmbeddingGenerationParams(
                    model = resolvedModel,
                    input = listOf(query),
                    dimensions = kb.embeddingDimensions,
                ),
            )
            result.embeddings.firstOrNull() ?: return@withContext emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to embed query", e)
            return@withContext emptyList()
        }

        val allEmbeddings = embeddingDao.getByKnowledgeBaseId(kb.id.toString())
        if (allEmbeddings.isEmpty()) return@withContext emptyList()

        val results = allEmbeddings.mapNotNull { entity ->
            try {
                val embedding = json.decodeFromString(
                    ListSerializer(Float.serializer()),
                    entity.embedding
                )
                val similarity = cosineSimilarity(queryEmbedding, embedding)
                if (similarity >= threshold) {
                    SearchResult(
                        chunkText = entity.chunkText,
                        similarity = similarity,
                        fileUri = entity.fileUri,
                        chunkIndex = entity.chunkIndex,
                    )
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse embedding for entity ${entity.id}", e)
                null
            }
        }

        results.sortedByDescending { it.similarity }.take(topK)
    }

    suspend fun isFileIndexed(kbId: String, fileUri: String): Boolean {
        return embeddingDao.getChunkCount(kbId, fileUri) > 0
    }

    suspend fun getIndexedFiles(kbId: String): List<String> {
        return embeddingDao.getIndexedFileUris(kbId)
    }

    private fun resolveModelAndProvider(
        kb: KnowledgeBase,
        model: Model?,
        providerSetting: ProviderSetting?,
    ): Pair<Model, ProviderSetting>? {
        if (model != null && providerSetting != null) return model to providerSetting
        val embeddingModelId = kb.embeddingModelId ?: return null
        return modelResolver.resolve(embeddingModelId)
    }

    private fun readFileContent(fileUri: String): String? {
        return try {
            val uri = Uri.parse(fileUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file: $fileUri", e)
            null
        }
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    data class SearchResult(
        val chunkText: String,
        val similarity: Float,
        val fileUri: String,
        val chunkIndex: Int,
    )
}
