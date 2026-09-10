package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "world_book_entries", primaryKeys = ["world_book_id", "entry_id"])
data class WorldBookEntryEntity(
    @ColumnInfo(name = "world_book_id") val worldBookId: String,
    @ColumnInfo(name = "entry_id") val entryId: String,
    val order: Int,
    val key: String,
    @ColumnInfo(name = "keys_json") val keysJson: String,
    val content: String,
    val enabled: Boolean,
    val probability: Int,
    val depth: Int,
    val selective: Boolean,
    val position: String,
)
