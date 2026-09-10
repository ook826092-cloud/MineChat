package cn.mine.minestars.data.ai.transformers

import cn.mine.ai.ui.UIMessage
import cn.mine.minestars.data.files.FilesManager
import org.koin.java.KoinJavaComponent.getKoin

object Base64ImageToLocalFileTransformer : OutputMessageTransformer {
    override suspend fun onGenerationFinish(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val filesManager = getKoin().get<FilesManager>()
        return messages.map { message ->
            filesManager.convertBase64ImagePartToLocalFile(message)
        }
    }
}
