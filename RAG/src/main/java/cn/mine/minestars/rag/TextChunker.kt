package cn.mine.minestars.rag

/**
 * 文本分块工具
 *
 * 将长文本按段落或固定长度分块，支持重叠以保持上下文连续性。
 */
object TextChunker {
    private const val DEFAULT_CHUNK_SIZE = 512
    private const val DEFAULT_OVERLAP = 128

    /**
     * 将文本分成块
     *
     * @param text 输入文本
     * @param chunkSize 每块最大字符数
     * @param overlap 重叠字符数
     * @return 分块列表
     */
    fun chunk(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP,
    ): List<String> {
        if (text.isBlank()) return emptyList()

        // 先按段落分割
        val paragraphs = text.split(Regex("\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (paragraphs.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            if (paragraph.length > chunkSize) {
                // 段落太大，需要进一步按固定长度分割
                if (currentChunk.isNotBlank()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
                chunks.addAll(splitLargeParagraph(paragraph, chunkSize, overlap))
            } else if (currentChunk.length + paragraph.length + 1 > chunkSize) {
                // 当前块加上新段落会超出限制，先保存当前块
                chunks.add(currentChunk.toString().trim())
                // 保留重叠部分
                val overlapText = getOverlapText(currentChunk.toString(), overlap)
                currentChunk.clear()
                if (overlapText.isNotBlank()) {
                    currentChunk.append(overlapText).append("\n")
                }
                currentChunk.append(paragraph)
            } else {
                if (currentChunk.isNotBlank()) {
                    currentChunk.append("\n\n")
                }
                currentChunk.append(paragraph)
            }
        }

        if (currentChunk.isNotBlank()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }

    private fun splitLargeParagraph(
        text: String,
        chunkSize: Int,
        overlap: Int,
    ): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            chunks.add(text.substring(start, end).trim())
            start += chunkSize - overlap
        }
        return chunks
    }

    private fun getOverlapText(text: String, overlap: Int): String {
        if (text.length <= overlap) return text
        return text.substring(text.length - overlap)
    }
}
