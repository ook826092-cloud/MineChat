package cn.mine.minestars.core.tavern.mapper

import org.json.JSONArray
import org.json.JSONObject

data class CharacterImportMapping(
    val characterName: String,
    val fields: CharacterCardFields,
    val worldBooks: List<Pair<String, String>>,
    val regexScripts: List<Pair<String, String>>,
    val regexGroupName: String
)

data class CharacterCardFields(
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMessage: String,
    val mesExamples: String,
    val systemPrompt: String,
    val postHistoryInstructions: String,
    val creatorNotes: String,
    val alternateGreetings: List<String>,
    val groupOnlyGreetings: List<String>,
    val nickname: String,
    val tags: List<String>,
    val creator: String,
    val characterVersion: String,
    val source: String,
)

object CharacterImportMapper {
    fun map(root: JSONObject, sourceFallbackName: String): CharacterImportMapping {
        val cardData = root.optJSONObject("data") ?: root
        val characterName = cardData.optString("name")
            .ifBlank { root.optString("name") }
            .ifBlank { sourceFallbackName }
            .ifBlank {
                error("Character card missing 'name' field")
            }
        val fields = extractFields(cardData, root, characterName)

        return CharacterImportMapping(
            characterName = characterName,
            fields = fields,
            worldBooks = extractWorldBooks(root, cardData, characterName),
            regexScripts = extractRegexScripts(root, cardData, characterName),
            regexGroupName = characterName
        )
    }

    private fun extractFields(
        cardData: JSONObject,
        root: JSONObject,
        characterName: String
    ): CharacterCardFields {
        return CharacterCardFields(
            name = characterName,
            description = cardData.optString("description"),
            personality = cardData.optString("personality"),
            scenario = cardData.optString("scenario"),
            firstMessage = cardData.optString("first_mes"),
            mesExamples = cardData.optString("mes_example"),
            systemPrompt = cardData.optString("system_prompt"),
            postHistoryInstructions = cardData.optString("post_history_instructions"),
            creatorNotes = cardData.optString("creator_notes"),
            alternateGreetings = cardData.optJSONArray("alternate_greetings").toStringList(),
            groupOnlyGreetings = cardData.optJSONArray("group_only_greetings").toStringList(),
            nickname = cardData.optString("nickname"),
            tags = cardData.optJSONArray("tags").toStringList(),
            creator = cardData.optString("creator"),
            characterVersion = cardData.optString("character_version"),
            source = cardData.optString("source")
                .ifBlank { root.optString("source") },
        )
    }

    private fun extractWorldBooks(
        root: JSONObject,
        cardData: JSONObject,
        characterName: String
    ): List<Pair<String, String>> {
        var baseWorld: JSONObject? = null
        val combinedEntries = JSONArray()

        fun mergeEntries(from: JSONObject?) {
            val entries = from?.optJSONArray("entries") ?: return
            if (baseWorld == null) {
                baseWorld = JSONObject(from.toString())
            }
            for (i in 0 until entries.length()) {
                combinedEntries.put(entries.opt(i))
            }
        }

        fun mergeEntriesFromArray(array: JSONArray?) {
            if (array == null) return
            for (i in 0 until array.length()) {
                val node = array.optJSONObject(i) ?: continue
                mergeEntries(node)
            }
        }

        // 1. character_book (SillyTavern V2/V3 standard)
        mergeEntries(cardData.optJSONObject("character_book"))

        // 2. Extension fields
        val extensions = cardData.optJSONObject("extensions")

        when (val extWorldbook = extensions?.opt("worldbook")) {
            is JSONObject -> mergeEntries(extWorldbook)
            is JSONArray -> mergeEntriesFromArray(extWorldbook)
        }

        when (val extLorebook = extensions?.opt("lorebook")) {
            is JSONObject -> mergeEntries(extLorebook)
            is JSONArray -> mergeEntriesFromArray(extLorebook)
        }

        mergeEntries(extensions?.optJSONObject("world"))

        if (combinedEntries.length() == 0) return emptyList()
        val finalWorld = baseWorld ?: JSONObject()
        finalWorld.put("entries", combinedEntries)
        return listOf(characterName to finalWorld.toString())
    }

    private fun extractRegexScripts(
        root: JSONObject,
        cardData: JSONObject,
        characterName: String
    ): List<Pair<String, String>> {
        val regexes = mutableListOf<Pair<String, String>>()

        // 1. Root-level regex_scripts
        val fromRoot = root.optJSONArray("regex_scripts")
        if (fromRoot != null) regexes += extractRegexFromArray(fromRoot, characterName)

        // 2. Data-level regex_scripts
        val fromData = cardData.optJSONArray("regex_scripts")
        if (fromData != null) regexes += extractRegexFromArray(fromData, characterName)

        // 3. Extensions
        val extensions = cardData.optJSONObject("extensions")
        val fromExtArray = extensions?.optJSONArray("regex_scripts")
        if (fromExtArray != null) regexes += extractRegexFromArray(fromExtArray, characterName)

        val fromExtObject = extensions?.optJSONObject("regex_scripts")
        if (fromExtObject != null) {
            val name = fromExtObject.optString("scriptName")
                .ifBlank { fromExtObject.optString("name") }
                .ifBlank { "$characterName-regex" }
            regexes += name to fromExtObject.toString()
        }

        // 4. Legacy extensions.regex
        val fromExtRegex = extensions?.optJSONArray("regex")
        if (fromExtRegex != null) regexes += extractRegexFromArray(fromExtRegex, characterName)

        return regexes
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optString(index).takeIf { it.isNotBlank() }
        }
    }

    private fun extractRegexFromArray(array: JSONArray, characterName: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        for (i in 0 until array.length()) {
            val node = array.optJSONObject(i) ?: continue
            val name = node.optString("scriptName")
                .ifBlank { node.optString("name") }
                .ifBlank { node.optString("title") }
                .ifBlank { node.optString("label") }
                .ifBlank { "$characterName-${i + 1}" }
            out += name to node.toString()
        }
        return out
    }
}
