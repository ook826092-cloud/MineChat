package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "preset_entries", primaryKeys = ["preset_id", "entry_index"])
data class PresetEntryEntity(
    @ColumnInfo(name = "preset_id") val presetId: String,
    @ColumnInfo(name = "entry_index") val entryIndex: Int,
    @ColumnInfo(name = "id") val id: String,
    val identifier: String,
    val name: String,
    val enabled: Boolean,
    val role: String,
    val content: String,
    @ColumnInfo(name = "injection_position") val injectionPosition: Int?,
    @ColumnInfo(name = "injection_depth") val injectionDepth: Int?,
    @ColumnInfo(name = "injection_order") val injectionOrder: Int?,
    @ColumnInfo(name = "system_prompt") val systemPrompt: Boolean,
    val marker: Boolean,
    @ColumnInfo(name = "forbid_overrides") val forbidOverrides: Boolean,
    @ColumnInfo(name = "injection_trigger_json") val injectionTriggerJson: String,
    val mounted: Boolean,
)
