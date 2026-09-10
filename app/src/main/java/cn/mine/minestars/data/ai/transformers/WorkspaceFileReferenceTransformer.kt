package cn.mine.minestars.data.ai.transformers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.mine.ai.ui.UIMessage
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.data.repository.WorkspaceRepository

class WorkspaceFileReferenceTransformer(
    private val workspaceRepository: WorkspaceRepository,
) : InputMessageTransformer {

    private val mentionRegex = Regex("@([\\w.\\-/]+)")

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val workspaceId = ctx.assistant.workspaceId ?: return messages
        val workspace = workspaceRepository.getWorkspace(workspaceId) ?: return messages

        return withContext(Dispatchers.IO) {
            messages.map { message ->
                message.copy(
                    parts = message.parts.map { part ->
                        if (part is UIMessagePart.Text) {
                            resolveMentions(part, workspace.root)
                        } else {
                            part
                        }
                    }
                )
            }
        }
    }

    private fun resolveMentions(part: UIMessagePart.Text, workspaceRoot: String): UIMessagePart {
        val text = part.text
        val matches = mentionRegex.findAll(text).toList()
        if (matches.isEmpty()) return part

        val sb = StringBuilder(text)
        // Process from end to start to preserve indices
        for (match in matches.reversed()) {
            val filePath = match.groupValues[1]
            val fileContent = runCatching {
                workspaceRepository.readFile(workspaceRoot, filePath)
            }.getOrNull()

            if (fileContent != null) {
                val replacement = """

                    ## user referenced a file: $filePath
                    <content>
                    ```
                    $fileContent
                    ```
                    </content>
                """.trimMargin()
                sb.replace(match.range.first, match.range.last + 1, replacement)
            }
        }
        return UIMessagePart.Text(sb.toString())
    }
}
