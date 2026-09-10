package cn.mine.minestars.data.import

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object JsonImportParser {
    fun readJsonObject(resolver: ContentResolver, uri: Uri): Pair<JSONObject, String?> {
        val displayName = queryDisplayName(resolver, uri)
        val text = resolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            ?: error("Failed to read file")
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) error("File is not a valid JSON object")
        return JSONObject(trimmed) to displayName
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameColumn = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0) cursor.getString(nameColumn) else null
        }
    }
}
