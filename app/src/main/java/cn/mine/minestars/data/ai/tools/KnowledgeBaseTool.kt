package cn.mine.minestars.data.ai.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import cn.mine.ai.core.InputSchema
import cn.mine.ai.core.Tool
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.rag.EmbeddingService
import cn.mine.minestars.rag.KnowledgeBase

fun createKnowledgeBaseTool(
    embeddingService: EmbeddingService,
    kb: KnowledgeBase,
): Tool = Tool(
    name = "search_knowledge_base",
    description = """
        Search the connected knowledge base for relevant information.
        Use this when the user asks about content that may be documented in the knowledge base.
        The knowledge base contains indexed documents; this tool performs semantic search.
    """.trimIndent(),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "Search query to find relevant documents in the knowledge base")
                }
                putJsonObject("top_k") {
                    put("type", "integer")
                    put("description", "Number of results to return (default: ${kb.topK})")
                    put("default", kb.topK)
                }
            },
            required = listOf("query")
        )
    },
    execute = { args ->
        val json = args.jsonObject
        val query = json["query"]?.jsonPrimitive?.content ?: return@Tool emptyList()
        val topK = json["top_k"]?.jsonPrimitive?.intOrNull ?: kb.topK

        val results = embeddingService.searchSimilar(
            kb = kb,
            query = query,
            topK = topK,
        )

        if (results.isEmpty()) {
            listOf(UIMessagePart.Text("未在知识库中找到相关信息"))
        } else {
            val text = buildString {
                appendLine("[来自知识库 \"${kb.name}\" 的搜索结果 (${results.size}条)]")
                results.forEachIndexed { index, result ->
                    appendLine()
                    appendLine("--- 结果 ${index + 1} ---")
                    appendLine("来源: ${result.fileUri.substringAfterLast("/")}")
                    appendLine("相似度: ${"%.2f".format(result.similarity)}")
                    appendLine(result.chunkText)
                }
            }
            listOf(UIMessagePart.Text(text))
        }
    }
)
