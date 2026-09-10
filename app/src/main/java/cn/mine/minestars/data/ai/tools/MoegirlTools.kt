package cn.mine.minestars.data.ai.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import cn.mine.ai.core.InputSchema
import cn.mine.ai.core.Tool
import cn.mine.ai.ui.UIMessagePart
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val API_BASE = "https://moegirl.icu/api.php"
private const val SITE_BASE = "https://moegirl.icu"

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

private fun moegirlGet(params: Map<String, String>): String {
    val qs = params.entries.joinToString("&") { (k, v) ->
        "$k=${java.net.URLEncoder.encode(v, "UTF-8")}"
    }
    val request = Request.Builder()
        .url("$API_BASE?$qs")
        .header("User-Agent", "Mozilla/5.0")
        .header("Accept", "*/*")
        .build()
    return httpClient.newCall(request).execute().use { response ->
        response.body?.string() ?: "{}"
    }
}

private fun String?.orEmpty(): String = this ?: ""

private fun cleanWikiText(text: String): String {
    return text
        .replace(Regex("<ref[^>]*>.*?</ref>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("'''(.*?)'''"), "**$1**")
        .replace(Regex("''(.*?)''"), "*$1*")
        .replace(Regex("\\[\\[([^|\\]]+)\\|([^\\]]+)\\]\\]"), "[$2]($1)")
        .replace(Regex("\\[\\[([^\\]]+)\\]\\]"), "[$1]($1)")
        .replace(Regex("(?m)^==\\s*(.*?)\\s*==$"), "## $1")
        .replace(Regex("(?m)^===\\s*(.*?)\\s*===$"), "### $1")
}

private fun extractHeadings(text: String): String {
    val headings = Regex("(?m)^(#{1,6})\\s+(.+)$").findAll(text).toList()
    if (headings.isEmpty()) return ""
    val sb = StringBuilder()
    sb.appendLine("📋 页面目录")
    sb.appendLine("=".repeat(20))
    headings.forEach { m ->
        val level = m.groupValues[1].length
        sb.appendLine("  ".repeat(level - 1) + "• " + m.groupValues[2])
    }
    sb.appendLine()
    return sb.toString()
}

private fun jsonInt(json: kotlinx.serialization.json.JsonElement?, default: Int = 0): Int {
    return json?.jsonPrimitive?.content?.toIntOrNull() ?: default
}

private fun jsonStr(json: kotlinx.serialization.json.JsonElement?, default: String = ""): String {
    return json?.jsonPrimitive?.content ?: default
}

// ════════════════════════════════════════════
// 1. moegirl_search
// ════════════════════════════════════════════
val moegirlSearchTool by lazy {
    Tool(
        name = "moegirl_search",
        description = "搜索ACG、二次元、动漫、游戏相关条目 — 萌娘百科专用搜索",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    putJsonObject("keyword") {
                        put("type", "string")
                        put("description", "搜索关键词")
                    }
                    putJsonObject("limit") {
                        put("type", "integer")
                        put("description", "返回结果数量，默认5")
                        put("default", 5)
                    }
                },
                required = listOf("keyword"),
            )
        },
        execute = { args ->
            val json = args.jsonObject
            val keyword = jsonStr(json["keyword"])
            if (keyword.isBlank()) return@Tool listOf(
                UIMessagePart.Text("❌ 未提供搜索关键词")
            )
            val limit = jsonInt(json["limit"], 5)

            val raw = moegirlGet(mapOf(
                "action" to "query",
                "format" to "json",
                "list" to "search",
                "srsearch" to keyword,
                "srlimit" to limit.toString(),
                "srprop" to "snippet",
            ))

            val data = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            if (data.containsKey("error")) {
                return@Tool listOf(UIMessagePart.Text("❌ API 错误: $raw"))
            }
            val items = data["query"]?.jsonObject?.get("search")?.jsonArray
            if (items == null || items.isEmpty()) {
                return@Tool listOf(UIMessagePart.Text("❌ 未找到相关条目"))
            }

            val sb = StringBuilder()
            sb.appendLine("🔍 萌娘百科搜索结果")
            sb.appendLine("=".repeat(20))
            for (i in 0 until items.size) {
                val obj = items[i].jsonObject
                val snippet = jsonStr(obj["snippet"]).replace(Regex("<[^>]*>"), "")
                sb.appendLine()
                sb.appendLine("${i + 1}. ${jsonStr(obj["title"])}")
                sb.appendLine("   页面ID: ${jsonInt(obj["pageid"])}")
                sb.appendLine("   链接: $SITE_BASE/index.php?curid=${jsonInt(obj["pageid"])}")
                sb.appendLine("   摘要: $snippet")
            }
            listOf(UIMessagePart.Text(sb.toString()))
        },
    )
}

// ════════════════════════════════════════════
// 2. moegirl_page
// ════════════════════════════════════════════
val moegirlPageTool by lazy {
    Tool(
        name = "moegirl_page",
        description = "获取ACG、二次元相关页面完整内容 — 萌娘百科页面获取，含自动目录",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    putJsonObject("pageid") {
                        put("type", "integer")
                        put("description", "页面ID（与 title 二选一）")
                    }
                    putJsonObject("title") {
                        put("type", "string")
                        put("description", "页面标题（与 pageid 二选一）")
                    }
                    putJsonObject("max_length") {
                        put("type", "integer")
                        put("description", "最大返回字符数，默认2000")
                        put("default", 2000)
                    }
                },
            )
        },
        execute = { args ->
            val json = args.jsonObject
            val pageid = jsonInt(json["pageid"])
            val title = jsonStr(json["title"])
            val maxLen = if (jsonInt(json["max_length"]) == 0) 2000 else jsonInt(json["max_length"], 2000)

            if (pageid == 0 && title.isBlank()) {
                return@Tool listOf(UIMessagePart.Text("❌ 请提供 pageid 或 title"))
            }

            val params = mutableMapOf(
                "action" to "parse",
                "format" to "json",
                "prop" to "wikitext",
            )
            if (pageid != 0) params["pageid"] = pageid.toString()
            else params["page"] = title

            val raw = moegirlGet(params)
            val data = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            if (data.containsKey("error")) {
                return@Tool listOf(UIMessagePart.Text("❌ API 错误: $raw"))
            }

            val parse = data["parse"]?.jsonObject ?: return@Tool listOf(
                UIMessagePart.Text("❌ 获取页面失败: 无 parse 数据")
            )
            val pageTitle = jsonStr(parse["title"], title.ifBlank { pageid.toString() })
            val content = jsonStr(parse["wikitext"]?.jsonObject?.get("*"))

            val cleaned = cleanWikiText(content)
            val toc = extractHeadings(cleaned)
            val body = if (cleaned.length > maxLen) cleaned.take(maxLen) else cleaned
            val suffix = if (cleaned.length > maxLen)
                "\n\n... (剩余 ${cleaned.length - maxLen} 字符)"
            else
                "\n\n(完整内容，共 ${cleaned.length} 字符)"

            listOf(UIMessagePart.Text("$toc📖 $pageTitle\n${"=".repeat(pageTitle.length + 3)}\n\n$body$suffix"))
        },
    )
}

// ════════════════════════════════════════════
// 3. moegirl_sections
// ════════════════════════════════════════════
val moegirlSectionsTool by lazy {
    Tool(
        name = "moegirl_sections",
        description = "获取ACG、二次元页面的指定段落或模板内容 — 萌娘百科页面段落提取",
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    putJsonObject("pageid") { put("type", "integer"); put("description", "页面ID") }
                    putJsonObject("title") { put("type", "string"); put("description", "页面标题") }
                    putJsonObject("section_titles") {
                        put("type", "array")
                        put("description", "要提取的段落标题列表")
                    }
                    putJsonObject("template_names") {
                        put("type", "array")
                        put("description", "要提取的模板名称列表")
                    }
                    putJsonObject("max_length") {
                        put("type", "integer")
                        put("description", "最大返回字符数，默认5000")
                        put("default", 5000)
                    }
                },
            )
        },
        execute = { args ->
            val json = args.jsonObject
            val pageid = jsonInt(json["pageid"])
            val title = jsonStr(json["title"])
            val sectionTitles = json["section_titles"]?.jsonArray?.map {
                it.jsonPrimitive.content
            } ?: emptyList()
            val templateNames = json["template_names"]?.jsonArray?.map {
                it.jsonPrimitive.content
            } ?: emptyList()
            val maxLen = if (jsonInt(json["max_length"]) == 0) 5000 else jsonInt(json["max_length"], 5000)

            if (sectionTitles.isEmpty() && templateNames.isEmpty()) {
                return@Tool listOf(UIMessagePart.Text("❌ 请提供 section_titles 或 template_names"))
            }
            if (pageid == 0 && title.isBlank()) {
                return@Tool listOf(UIMessagePart.Text("❌ 请提供 pageid 或 title"))
            }

            val params = mutableMapOf(
                "action" to "parse",
                "format" to "json",
                "prop" to "wikitext",
            )
            if (pageid != 0) params["pageid"] = pageid.toString()
            else params["page"] = title

            val raw = moegirlGet(params)
            val data = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            if (data.containsKey("error")) {
                return@Tool listOf(UIMessagePart.Text("❌ API 错误: $raw"))
            }

            val content = jsonStr(data["parse"]?.jsonObject?.get("wikitext")?.jsonObject?.get("*"))
            val lines = content.split("\n")
            val results = mutableListOf<String>()

            for (st in sectionTitles) {
                var found = false
                var headingLevel = 0
                val buf = mutableListOf<String>()
                for (line in lines) {
                    val hm = Regex("^(=+)\\s*(.*?)\\s*\\1\\s*$").find(line)
                    if (hm != null) {
                        val level = hm.groupValues[1].length
                        val hTitle = hm.groupValues[2]
                        if (!found && st.lowercase() in hTitle.lowercase()) {
                            found = true
                            headingLevel = level
                            buf.add("📖 $hTitle")
                            buf.add("=".repeat(hTitle.length + 3))
                            continue
                        }
                        if (found && level <= headingLevel) break
                    }
                    if (found) buf.add(line)
                }
                if (found) results.add(buf.joinToString("\n"))
            }

            for (tn in templateNames) {
                val pattern = Regex("\\{\\{${Regex.escape(tn)}.*?\\}\\}", RegexOption.DOT_MATCHES_ALL)
                pattern.findAll(content).forEach { match ->
                    results.add("🔧 模板: $tn\n${"=".repeat(tn.length + 5)}\n\n${match.value}")
                }
            }

            if (results.isEmpty()) {
                return@Tool listOf(UIMessagePart.Text(
                    "❌ 未找到匹配的标题或模板\n标题: ${sectionTitles.joinToString()}\n模板: ${templateNames.joinToString()}"
                ))
            }

            val combined = results.joinToString("\n\n${"-".repeat(50)}\n\n")
            val body = if (combined.length > maxLen)
                combined.take(maxLen) + "\n\n... (剩余 ${combined.length - maxLen} 字符)"
            else combined
            listOf(UIMessagePart.Text(body))
        },
    )
}

fun getMoegirlTools(): List<Tool> = listOf(moegirlSearchTool, moegirlPageTool, moegirlSectionsTool)
