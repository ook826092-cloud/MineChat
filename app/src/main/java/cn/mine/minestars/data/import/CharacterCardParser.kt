package cn.mine.minestars.data.import

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import org.json.JSONObject

data class ParsedCharacterCard(
    val fileName: String,
    val root: JSONObject,
    val normalizedJsonText: String,
    val importedAvatarUri: String?
)

object CharacterCardParser {
    fun parse(resolver: ContentResolver, uri: Uri): ParsedCharacterCard {
        val fileName = queryDisplayName(resolver, uri)
            ?: uri.lastPathSegment
            ?: "unknown"
        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Failed to read file")

        val jsonText = parseCardJsonText(fileName = fileName, rawBytes = rawBytes)
        val root = JSONObject(jsonText)
        val importedAvatar = if (fileName.endsWith(".png", ignoreCase = true)) uri.toString() else null
        return ParsedCharacterCard(
            fileName = fileName,
            root = root,
            normalizedJsonText = jsonText,
            importedAvatarUri = importedAvatar,
        )
    }

    private fun parseCardJsonText(fileName: String, rawBytes: ByteArray): String {
        return if (fileName.endsWith(".png", ignoreCase = true)) {
            val text = extractJsonFromPng(rawBytes)
                ?: throw IllegalArgumentException("No JSON found in PNG chunk")
            // SillyTavern: PNG chunks store base64-encoded JSON
            val decoded = Base64.decode(text, Base64.DEFAULT)
            decoded.decodeToString()
        } else {
            // SillyTavern: JSON files are plain UTF-8 text
            rawBytes.decodeToString()
        }
    }

    internal fun extractJsonFromPng(rawBytes: ByteArray): String? {
        var offset = 8 // skip PNG signature
        while (offset + 8 <= rawBytes.size) {
            val length = ((rawBytes[offset].toInt() and 0xFF) shl 24) or
                    ((rawBytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((rawBytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (rawBytes[offset + 3].toInt() and 0xFF)
            val chunkType = String(rawBytes, offset + 4, 4, Charsets.US_ASCII)
            if (chunkType == "zTXt" || chunkType == "tEXt") {
                val dataOffset = offset + 8
                val dataEnd = dataOffset + length
                if (dataEnd > rawBytes.size) return null
                val chunkData = rawBytes.sliceArray(dataOffset until dataEnd)
                val nullIndex = chunkData.indexOf(0)
                if (nullIndex < 0) return null
                val keyword = String(chunkData, 0, nullIndex, Charsets.ISO_8859_1)
                val textBytes = chunkData.sliceArray((nullIndex + 1) until chunkData.size)
                val text = if (chunkType == "zTXt") {
                    if (textBytes.isEmpty()) return null
                    val compressionMethod = textBytes[0].toInt() and 0xFF
                    if (compressionMethod != 0) return null
                    val compressed = textBytes.sliceArray(1 until textBytes.size)
                    java.util.zip.Inflater().run {
                        setInput(compressed)
                        val buf = java.io.ByteArrayOutputStream()
                        val tmp = ByteArray(4096)
                        while (!finished()) {
                            val count = inflate(tmp)
                            buf.write(tmp, 0, count)
                        }
                        end()
                        buf.toString("UTF-8")
                    }
                } else {
                    textBytes.decodeToString()
                }
                if (keyword.equals("chara", ignoreCase = true) || keyword.equals("ccv3", ignoreCase = true)) {
                    return text
                }
            }
            offset += 12 + length
        }
        return null
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0) cursor.getString(nameColumn) else null
            } else null
        }
    }
}
