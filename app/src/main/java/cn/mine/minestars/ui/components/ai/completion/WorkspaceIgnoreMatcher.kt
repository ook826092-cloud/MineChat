package cn.mine.minestars.ui.components.ai.completion

class WorkspaceIgnoreMatcher(rules: String?) {
    private val patterns: List<IgnorePattern>

    init {
        patterns = rules?.lines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.map { parsePattern(it) }
            ?: emptyList()
    }

    fun accept(path: String): Boolean {
        var result = true
        for (pattern in patterns) {
            if (pattern.matcher.matches(path)) {
                result = !pattern.isNegated
            }
        }
        return result
    }

    fun isEmpty(): Boolean = patterns.isEmpty()

    companion object {
        private data class IgnorePattern(
            val matcher: Regex,
            val isNegated: Boolean,
            val isDirectoryOnly: Boolean,
        )

        fun default(): WorkspaceIgnoreMatcher = WorkspaceIgnoreMatcher(
            buildString {
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
            }
        )

        private fun parsePattern(pattern: String): IgnorePattern {
            var p = pattern
            val isNegated = p.startsWith("!")
            if (isNegated) p = p.substring(1)

            val isDirectoryOnly = p.endsWith("/")
            if (isDirectoryOnly) p = p.substring(0, p.length - 1)

            val isAnchored = p.startsWith("/")
            if (isAnchored) p = p.substring(1)

            val regexStr = buildRegexPattern(p, isAnchored)
            val suffix = if (isDirectoryOnly) "(/.*)?$" else "$"
            val finalRegex = if (regexStr.endsWith("$")) {
                regexStr.dropLast(1) + suffix
            } else {
                regexStr + suffix
            }

            return IgnorePattern(
                matcher = Regex(finalRegex),
                isNegated = isNegated,
                isDirectoryOnly = isDirectoryOnly,
            )
        }

        private fun buildRegexPattern(pattern: String, isAnchored: Boolean): String {
            val glob = globToRegex(pattern)
            return if (isAnchored) {
                "^$glob$"
            } else if (pattern.contains("/")) {
                "(^|.*/)$glob$"
            } else {
                "(^|.*/)$glob$"
            }
        }

        private fun globToRegex(pattern: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < pattern.length) {
                when (val c = pattern[i]) {
                    '.' -> sb.append("\\.")
                    '*' -> {
                        if (i + 1 < pattern.length && pattern[i + 1] == '*') {
                            sb.append(".*")
                            i++
                        } else {
                            sb.append("[^/]*")
                        }
                    }
                    '?' -> sb.append("[^/]")
                    '\\' -> {
                        sb.append("\\\\")
                        i++
                    }
                    else -> sb.append(c)
                }
                i++
            }
            return sb.toString()
        }
    }
}
