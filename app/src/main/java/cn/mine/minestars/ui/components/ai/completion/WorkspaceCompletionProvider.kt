package cn.mine.minestars.ui.components.ai.completion

import androidx.compose.ui.text.TextRange
import cn.mine.minestars.data.repository.WorkspaceRepository

class WorkspaceCompletionProvider(
    override val id: String,
    private val workspaceRoot: String,
    private val repository: WorkspaceRepository,
) : ChatCompletionProvider {

    private data class Entry(
        val path: String,
        val name: String,
        val isDirectory: Boolean,
    )

    private data class CachedEntries(
        val timestamp: Long,
        val entries: List<Entry>,
    )

    private var cachedEntries: CachedEntries? = null

    override suspend fun complete(context: ChatCompletionContext): ChatCompletionList? {
        val mention = findWorkspaceMention(context) ?: return null
        val entries = loadEntries()
        if (entries.isEmpty()) return null

        val query = mention.query
        val scored = entries.mapNotNull { entry ->
            val score = matchScore(query, entry.path, entry.name)
            if (score > 0) {
                ChatCompletionItem(
                    label = entry.name,
                    insertText = if (entry.isDirectory) "${entry.path}/" else "${entry.path} ",
                    detail = entry.path,
                    sortScore = score + if (entry.isDirectory) DIRECTORY_SCORE_BONUS else 0,
                )
            } else null
        }

        val sorted = scored
            .sortedWith(
                compareByDescending<ChatCompletionItem> { it.sortScore }
                    .thenBy { it.label.length }
                    .thenBy { it.label }
            )
            .take(MAX_COMPLETION_ITEMS)

        return ChatCompletionList(
            providerId = id,
            replacementRange = TextRange(mention.start, mention.end),
            items = sorted,
        )
    }

    private data class MentionResult(
        val start: Int,
        val end: Int,
        val query: String,
    )

    /**
     * Find @ mention before cursor with boundary character check.
     * @ is only valid when preceded by whitespace or opening brackets/quotes.
     */
    private fun findWorkspaceMention(context: ChatCompletionContext): MentionResult? {
        val text = context.text
        val cursor = context.cursor
        if (cursor <= 0 || cursor > text.length) return null

        val beforeCursor = text.substring(0, cursor)
        val atIndex = beforeCursor.lastIndexOf('@')
        if (atIndex < 0) return null

        // Check boundary character before @
        if (atIndex > 0) {
            val prev = text[atIndex - 1]
            if (!prev.isWhitespace() && prev !in "([{<\"'") return null
        }

        // Extract query (text after @ until whitespace or end of text before cursor)
        val afterAt = beforeCursor.substring(atIndex + 1)

        return MentionResult(
            start = atIndex,
            end = cursor,
            query = afterAt,
        )
    }

    /**
     * BFS traversal of workspace files with gitignore filtering and caching.
     */
    private fun loadEntries(): List<Entry> {
        val now = System.currentTimeMillis()
        cachedEntries?.let { (timestamp, entries) ->
            if (now - timestamp < CACHE_TTL_MILLIS) return entries
        }

        val entries = mutableListOf<Entry>()
        val dirQueue = ArrayDeque<String>()
        dirQueue.add("")

        // Build ignore matcher with defaults + root .gitignore
        val ignoreRules = buildString {
            // Default ignore patterns
            appendLine("build/")
            appendLine(".gradle/")
            appendLine(".git/")
            appendLine("node_modules/")
            appendLine("dist/")
            appendLine("out/")
            appendLine(".idea/")
            appendLine(".DS_Store")
            appendLine("*.class")
            appendLine("*.log")
            // Project-specific gitignore rules
            runCatching {
                val gitignore = repository.readFile(workspaceRoot, ".gitignore")
                appendLine(gitignore)
            }
        }
        val matcher = WorkspaceIgnoreMatcher(ignoreRules)

        var totalEntries = 0
        var totalDirs = 0

        while (dirQueue.isNotEmpty() && totalEntries < MAX_INDEXED_ENTRIES && totalDirs < MAX_INDEXED_DIRS) {
            val dir = dirQueue.removeFirst()
            if (dir.isNotEmpty()) totalDirs++

            val fileList = try {
                repository.listFiles(workspaceRoot, dir)
            } catch (_: Exception) {
                continue
            }

            for (file in fileList) {
                val relativePath = if (dir.isEmpty()) file.name else "$dir/${file.name}"
                if (!matcher.accept(relativePath)) continue

                if (file.isDirectory) {
                    dirQueue.add(relativePath)
                } else {
                    entries.add(Entry(relativePath, file.name, isDirectory = false))
                    totalEntries++
                }
            }
        }

        cachedEntries = CachedEntries(now, entries)
        return entries
    }

    companion object {
        private const val MAX_COMPLETION_ITEMS = 8
        private const val MAX_INDEXED_ENTRIES = 500
        private const val MAX_INDEXED_DIRS = 80
        private const val CACHE_TTL_MILLIS = 5000L
        private const val DIRECTORY_SCORE_BONUS = 25

        /**
         * Fuzzy scoring function:
         * - exact match (basename)           = 1000
         * - exact match (full path)          = 1000
         * - starts with (basename)           = 900 - indexOf
         * - starts with (any segment)        = 900 - indexOf
         * - contains (basename)              = 800
         * - contains (any segment)           = 800
         * - subsequence (full path)          = 500 - span
         * - subsequence (basename)           = 500 - span
         * - no match                          = 0
         */
        private fun matchScore(query: String, path: String, name: String): Int {
            if (query.isEmpty()) return 1

            val lowerQuery = query.lowercase()
            val lowerPath = path.lowercase()
            val lowerName = name.lowercase()

            // Exact match (basename)
            if (lowerName == lowerQuery) return 1000

            // Exact match (full path)
            if (lowerPath == lowerQuery) return 1000

            // Starts with (basename)
            if (lowerName.startsWith(lowerQuery)) {
                return 900
            }

            // Starts with (any path segment)
            val segments = lowerPath.split("/")
            for (segment in segments) {
                val idx = segment.indexOf(lowerQuery)
                if (idx == 0) {
                    return 900
                }
            }

            // Contains (basename)
            if (lowerName.contains(lowerQuery)) return 800

            // Contains (any path segment)
            for (segment in segments) {
                if (segment.contains(lowerQuery)) return 800
            }

            // Subsequence match on full path
            val pathSubScore = subsequenceScore(lowerQuery, lowerPath)
            if (pathSubScore > 0) return pathSubScore

            // Subsequence match on basename
            val nameSubScore = subsequenceScore(lowerQuery, lowerName)
            if (nameSubScore > 0) return nameSubScore

            return 0
        }

        /**
         * Compute subsequence score: how well the query characters appear in order within the target.
         * Score = 500 - span (distance between first and last matched character).
         */
        private fun subsequenceScore(query: String, target: String): Int {
            var qi = 0
            var firstMatch = -1
            var lastMatch = -1

            for (ti in target.indices) {
                if (qi < query.length && target[ti] == query[qi]) {
                    if (firstMatch < 0) firstMatch = ti
                    lastMatch = ti
                    qi++
                }
            }

            return if (qi == query.length) {
                val span = lastMatch - firstMatch
                (500 - span).coerceAtLeast(1)
            } else {
                0
            }
        }
    }
}
