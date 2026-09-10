package cn.mine.minestars.data.ai.transformers

import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.Loader
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import cn.mine.ai.ui.UIMessage
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.utils.toLocalDate
import cn.mine.minestars.utils.toLocalTime
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import kotlin.time.toJavaInstant

class TemplateTransformer(
    private val engine: PebbleEngine,
    private val settingsStore: SettingsStore
) : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val template = engine.getTemplate(ctx.assistant.id.toString())
        val timeZone = TimeZone.currentSystemDefault()
        return messages.map { message ->
            // 使用消息本身的发送时间而不是当前时间, 保证多次请求时渲染结果稳定, 不破坏 prompt 缓存
            val createdAt = message.createdAt.toInstant(timeZone).toJavaInstant()
            message.copy(
                parts = message.parts.map { part ->
                    when (part) {
                        is UIMessagePart.Text -> {
                            val result = StringWriter()
                            template.evaluate(
                                result, mapOf(
                                    "message" to part.text,
                                    "role" to message.role.name.lowercase(),
                                    "time" to createdAt.toLocalTime(),
                                    "date" to createdAt.toLocalDate(),
                                )
                            )
                            part.copy(
                                text = result.toString()
                            )
                        }

                        else -> part
                    }
                }
            )
        }
    }
}

class AssistantTemplateLoader(
    private val settingsStore: SettingsStore,
    private val assistantDAO: AssistantDAO,
) : Loader<String> {
    override fun getReader(cacheKey: String?): Reader? {
        val assistants = kotlinx.coroutines.runBlocking {
            assistantDAO.getAll().map { it.toModel() }
        }
        val content = assistants
            .find { it.id.toString() == cacheKey }?.messageTemplate
            ?: return null
        return StringReader(content)
    }

    override fun setCharset(charset: String?) {}

    override fun setPrefix(prefix: String?) {}

    override fun setSuffix(suffix: String?) {}

    override fun resolveRelativePath(
        relativePath: String?,
        anchorPath: String?
    ): String? {
        return relativePath
    }

    override fun createCacheKey(templateName: String?): String? {
        return templateName
    }

    override fun resourceExists(templateName: String?): Boolean {
        val assistants = kotlinx.coroutines.runBlocking {
            assistantDAO.getAll().map { it.toModel() }
        }
        return assistants.any { it.id.toString() == templateName }
    }
}
