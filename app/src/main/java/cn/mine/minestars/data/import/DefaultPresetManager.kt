package cn.mine.minestars.data.import

import cn.mine.minestars.core.tavern.preset.DefaultPresetFactory
import cn.mine.minestars.core.tavern.preset.PresetParser
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.entity.PresetEntity
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import org.json.JSONArray
import org.json.JSONObject

object DefaultPresetManager {

    suspend fun ensureDefaultPresetExists(
        presetDao: PresetDAO,
        presetEntryDao: PresetEntryDAO,
    ) {
        val existing = presetDao.getDefaultPreset()
        if (existing != null) return

        val rawJson = DefaultPresetFactory.createDefaultPresetJson()
        val presetId = DefaultPresetFactory.DEFAULT_PRESET_ID
        val presetObj = JSONObject(rawJson)

        val entries = PresetParser.extractEntries(presetObj)
        val entryEntities = entries.mapIndexed { index, entry ->
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
                mounted = entry.mounted,
            )
        }

        presetDao.insert(
            PresetEntity(
                id = presetId,
                name = "默认预设",
                rawJson = rawJson,
                isDefault = true,
            )
        )
        if (entryEntities.isNotEmpty()) {
            presetEntryDao.insertAll(entryEntities)
        }
    }
}
