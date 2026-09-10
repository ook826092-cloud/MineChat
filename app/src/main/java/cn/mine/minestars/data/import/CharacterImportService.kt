package cn.mine.minestars.data.import

import android.content.ContentResolver
import android.net.Uri
import cn.mine.minestars.core.tavern.mapper.CharacterCardFields
import cn.mine.minestars.core.tavern.mapper.CharacterImportMapper
import cn.mine.minestars.core.tavern.preset.PresetParser
import cn.mine.minestars.data.db.dao.CharacterCardDAO
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.dao.RegexGroupDAO
import cn.mine.minestars.data.db.dao.RegexScriptDAO
import cn.mine.minestars.data.db.dao.WorldBookDAO
import cn.mine.minestars.data.db.entity.CharacterCardEntity
import cn.mine.minestars.data.db.entity.PresetEntity
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.data.db.entity.RegexGroupEntity
import cn.mine.minestars.data.db.entity.RegexScriptEntity
import cn.mine.minestars.data.db.entity.WorldBookEntity
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.context.GlobalContext
import java.util.UUID

data class CharacterImportResult(
    val characterName: String,
    val worldBookCount: Int,
    val regexCount: Int,
    val characterCardId: String,
    val fields: CharacterCardFields,
    val worldBookIds: List<String>,
    val regexScriptIds: List<String>,
    val regexGroupId: String?,
    val avatarUri: String?,
    val worldBookJson: String? = null,
    val regexScriptsJson: String? = null,
)

object CharacterImportService {
    private val characterCardDao: CharacterCardDAO by lazy {
        GlobalContext.get().get()
    }
    private val worldBookDao: WorldBookDAO by lazy {
        GlobalContext.get().get()
    }
    private val regexScriptDao: RegexScriptDAO by lazy {
        GlobalContext.get().get()
    }
    private val regexGroupDao: RegexGroupDAO by lazy {
        GlobalContext.get().get()
    }
    private val presetDao: PresetDAO by lazy {
        GlobalContext.get().get()
    }
    private val presetEntryDao: PresetEntryDAO by lazy {
        GlobalContext.get().get()
    }
    suspend fun importCharacterCard(
        resolver: ContentResolver,
        uri: Uri,
        assistantId: String? = null,
    ): CharacterImportResult {
        val parsed = CharacterCardParser.parse(resolver, uri)
        val fallbackName = parsed.fileName.substringBeforeLast(".")
        val mapping = CharacterImportMapper.map(parsed.root, fallbackName)

        val linkedWorldIds: List<String>
        val linkedRegexIds: List<String>
        val regexGroupId: String?

        if (assistantId != null) {
            // ── Layer 2 mode: return world books and regex as embedded JSON;
            //    caller sets them on the Assistant before persisting ──
            val wbJson = buildWorldBookJson(mapping.worldBooks)
            val reJson = buildRegexScriptsJson(mapping.regexScripts)
            linkedWorldIds = emptyList()
            linkedRegexIds = emptyList()
            regexGroupId = null
            return CharacterImportResult(
                characterName = mapping.characterName,
                worldBookCount = linkedWorldIds.distinct().size,
                regexCount = linkedRegexIds.distinct().size,
                characterCardId = "",
                fields = mapping.fields,
                worldBookIds = linkedWorldIds.distinct(),
                regexScriptIds = linkedRegexIds.distinct(),
                regexGroupId = regexGroupId,
                avatarUri = parsed.importedAvatarUri,
                worldBookJson = wbJson,
                regexScriptsJson = reJson,
            )
        } else {
            // ── Legacy mode: store globally (for CharacterCardVM / card-list import) ──
            linkedWorldIds = mapping.worldBooks.map { (name, json) ->
                upsertWorldBook(name = name, rawJson = json)
            }
            linkedRegexIds = mapping.regexScripts.map { (name, json) ->
                upsertRegex(name = name, rawJson = json, scope = "SCOPED")
            }
            regexGroupId = if (linkedRegexIds.isNotEmpty()) {
                upsertRegexGroup(
                    name = mapping.regexGroupName,
                    characterName = mapping.characterName,
                    regexIds = linkedRegexIds,
                    scope = "SCOPED"
                )
            } else {
                null
            }
        }

        val characterId = upsertCharacter(
            name = mapping.characterName,
            sourceFileName = parsed.fileName,
            rawJson = parsed.normalizedJsonText,
            linkedWorldBookIds = linkedWorldIds,
            linkedRegexIds = linkedRegexIds,
            linkedRegexGroupId = regexGroupId
        )

        return CharacterImportResult(
            characterName = mapping.characterName,
            worldBookCount = linkedWorldIds.distinct().size,
            regexCount = linkedRegexIds.distinct().size,
            characterCardId = characterId,
            fields = mapping.fields,
            worldBookIds = linkedWorldIds.distinct(),
            regexScriptIds = linkedRegexIds.distinct(),
            regexGroupId = regexGroupId,
            avatarUri = parsed.importedAvatarUri
        )
    }

    suspend fun importPresets(resolver: ContentResolver, uri: Uri): Int {
        val (json, fileName) = JsonImportParser.readJsonObject(resolver, uri)
        val fileBaseName = fileName?.substringBeforeLast(".")?.ifBlank { null }
        val imported = mutableListOf<String>()

        val presetArray = json.optJSONArray("presets")
        if (presetArray != null) {
            for (i in 0 until presetArray.length()) {
                val node = presetArray.optJSONObject(i) ?: continue
                val name = node.optString("name").ifBlank {
                    fileBaseName?.let { if (presetArray.length() == 1) it else "$it-${i + 1}" }
                        ?: "Preset ${i + 1}"
                }
                upsertPreset(name = name, rawJson = node.toString(), presetObj = node)
                imported += name
            }
        } else {
            val existingPresets = presetDao.getAll()
            val name = json.optString("name").ifBlank {
                fileBaseName ?: "Preset ${existingPresets.size + 1}"
            }
            upsertPreset(name = name, rawJson = json.toString(), presetObj = json)
            imported += name
        }

        return imported.distinct().size
    }

    suspend fun importWorldBooks(resolver: ContentResolver, uri: Uri): Int {
        val (json, _) = JsonImportParser.readJsonObject(resolver, uri)
        val imported = mutableListOf<String>()

        val entries = json.optJSONArray("entries")
        if (entries != null || json.has("name")) {
            val existingWorldBooks = worldBookDao.getAll()
            val name = json.optString("name").ifBlank { "World Book ${existingWorldBooks.size + 1}" }
            upsertWorldBook(name, json.toString())
            imported += name
        }

        val books = json.optJSONArray("worldbooks")
        if (books != null) {
            for (i in 0 until books.length()) {
                val node = books.optJSONObject(i) ?: continue
                val name = node.optString("name").ifBlank { "World Book ${i + 1}" }
                upsertWorldBook(name, node.toString())
                imported += name
            }
        }

        return imported.distinct().size
    }

    suspend fun importRegexScripts(resolver: ContentResolver, uri: Uri): Int {
        val (json, fileName) = JsonImportParser.readJsonObject(resolver, uri)
        val fileBaseName = fileName?.substringBeforeLast(".")?.ifBlank { null }
        val imported = mutableListOf<String>()
        val regexIds = mutableListOf<String>()

        val regexArray = json.optJSONArray("regex_scripts")
        if (regexArray != null) {
            extractRegexFromArray(regexArray, "Imported").forEach { (name, raw) ->
                regexIds += upsertRegex(name, raw, "IMPORTED")
                imported += name
            }
        } else {
            val name = json.optString("scriptName")
                .ifBlank { json.optString("name") }
                .ifBlank { "Regex" }
            regexIds += upsertRegex(name, json.toString(), "IMPORTED")
            imported += name
        }

        if (regexIds.isNotEmpty()) {
            val groupName = fileBaseName ?: "Imported Regex Group"
            upsertRegexGroup(
                name = groupName,
                characterName = null,
                regexIds = regexIds,
                scope = "IMPORTED"
            )
        }

        return imported.distinct().size
    }

    private fun buildWorldBookJson(worldBooks: List<Pair<String, String>>): String? {
        if (worldBooks.isEmpty()) return null
        // Use the first world book (typically the character card's embedded book)
        val (name, json) = worldBooks.first()
        // Try to extract entries from the raw Tavern JSON
        val entries = runCatching {
            val obj = JSONObject(json)
            obj.optJSONArray("entries") ?: obj.optJSONObject("character_book")?.optJSONArray("entries")
                ?: obj.optJSONObject("data")?.optJSONArray("entries")
        }.getOrNull()
        val result = JSONObject()
        result.put("name", name)
        result.put("enabled", true)
        result.put("entries", entries ?: JSONArray())
        return result.toString()
    }

    private fun buildRegexScriptsJson(scripts: List<Pair<String, String>>): String? {
        if (scripts.isEmpty()) return null
        val arr = JSONArray()
        scripts.forEachIndexed { index, (name, rawJson) ->
            val obj = JSONObject()
            obj.put("id", UUID.randomUUID().toString())
            obj.put("name", name)
            obj.put("rawJson", rawJson)
            obj.put("sortOrder", index)
            obj.put("enabled", true)
            arr.put(obj)
        }
        val root = JSONObject()
        root.put("enabled", true)
        root.put("scripts", arr)
        return root.toString()
    }

    private suspend fun upsertWorldBook(
        name: String,
        rawJson: String,
    ): String {
        // Global: dedup by name
        val existing = worldBookDao.getAll()
        val existingMatch = existing.find { it.name == name }
        if (existingMatch != null) {
            worldBookDao.insert(existingMatch.copy(rawJson = rawJson))
            return existingMatch.id
        }
        val id = UUID.randomUUID().toString()
        worldBookDao.insert(WorldBookEntity(id = id, name = name, rawJson = rawJson))
        return id
    }

    private suspend fun upsertRegex(name: String, rawJson: String, scope: String): String {
        val existing = regexScriptDao.getAll()
        val existingMatch = existing.find { it.name == name }
        if (existingMatch != null) {
            regexScriptDao.insertAll(listOf(existingMatch.copy(rawJson = rawJson, scope = scope)))
            return existingMatch.id
        }
        val id = UUID.randomUUID().toString()
        regexScriptDao.insertAll(listOf(RegexScriptEntity(id = id, name = name, rawJson = rawJson, scope = scope, groupId = null)))
        return id
    }

    private suspend fun upsertRegexGroup(
        name: String,
        characterName: String?,
        regexIds: List<String>,
        scope: String = "SCOPED"
    ): String {
        val id = UUID.randomUUID().toString()
        val regexIdsJson = JSONArray(regexIds).toString()
        regexGroupDao.insertAll(
            listOf(
                RegexGroupEntity(
                    id = id,
                    name = name,
                    characterName = characterName,
                    regexIdsJson = regexIdsJson,
                    scope = scope
                )
            )
        )
        return id
    }

    private suspend fun upsertCharacter(
        name: String,
        sourceFileName: String,
        rawJson: String,
        linkedWorldBookIds: List<String>,
        linkedRegexIds: List<String>,
        linkedRegexGroupId: String?
    ): String {
        val id = UUID.randomUUID().toString()
        characterCardDao.insert(
            CharacterCardEntity(
                id = id,
                name = name,
                sourceFileName = sourceFileName,
                rawJson = rawJson,
                linkedWorldBookIdsJson = JSONArray(linkedWorldBookIds).toString(),
                linkedRegexIdsJson = JSONArray(linkedRegexIds).toString(),
                linkedRegexGroupId = linkedRegexGroupId
            )
        )
        return id
    }

    private suspend fun upsertPreset(name: String, rawJson: String, presetObj: JSONObject) {
        val existing = presetDao.getAll()
        val existingMatch = existing.find { it.name == name }

        if (existingMatch != null) {
            presetEntryDao.deleteByPresetId(existingMatch.id)
            val entries = PresetParser.extractEntries(presetObj)
            val presetEntryEntities = entries.mapIndexed { index, entry ->
                PresetEntryEntity(
                    presetId = existingMatch.id,
                    entryIndex = index,
                    id = entry.id,
                    identifier = entry.identifier,
                    name = entry.name,
                    enabled = entry.enabled,
                    role = entry.role,
                    content = entry.content,
                    injectionPosition = entry.injectionPosition,
                    injectionDepth = entry.injectionDepth,
                    injectionOrder = entry.injectionOrder,
                    systemPrompt = entry.systemPrompt,
                    marker = entry.marker,
                    forbidOverrides = entry.forbidOverrides,
                    injectionTriggerJson = JSONArray(entry.injectionTrigger).toString(),
                    mounted = entry.mounted
                )
            }
            presetDao.insert(existingMatch.copy(rawJson = rawJson))
            if (presetEntryEntities.isNotEmpty()) {
                presetEntryDao.insertAll(presetEntryEntities)
            }
        } else {
            val presetId = UUID.randomUUID().toString()
            val entries = PresetParser.extractEntries(presetObj)
            val presetEntryEntities = entries.mapIndexed { index, entry ->
                PresetEntryEntity(
                    presetId = presetId,
                    entryIndex = index,
                    id = entry.id,
                    identifier = entry.identifier,
                    name = entry.name,
                    enabled = entry.enabled,
                    role = entry.role,
                    content = entry.content,
                    injectionPosition = entry.injectionPosition,
                    injectionDepth = entry.injectionDepth,
                    injectionOrder = entry.injectionOrder,
                    systemPrompt = entry.systemPrompt,
                    marker = entry.marker,
                    forbidOverrides = entry.forbidOverrides,
                    injectionTriggerJson = JSONArray(entry.injectionTrigger).toString(),
                    mounted = entry.mounted
                )
            }
            presetDao.insert(
                PresetEntity(
                    id = presetId,
                    name = name,
                    rawJson = rawJson,
                    isDefault = false
                )
            )
            if (presetEntryEntities.isNotEmpty()) {
                presetEntryDao.insertAll(presetEntryEntities)
            }
        }
    }

    private fun extractRegexFromArray(
        array: org.json.JSONArray,
        characterName: String
    ): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        for (i in 0 until array.length()) {
            val node = array.optJSONObject(i) ?: continue
            val name = node.optString("scriptName")
                .ifBlank { node.optString("name") }
                .ifBlank { "$characterName-${i + 1}" }
            out += name to node.toString()
        }
        return out
    }
}
