package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "assistant_tags", primaryKeys = ["assistant_id", "tag_id"])
data class AssistantTagEntity(
    @ColumnInfo(name = "assistant_id") val assistantId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
)
