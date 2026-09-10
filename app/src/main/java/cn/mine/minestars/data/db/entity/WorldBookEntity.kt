package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_books")
data class WorldBookEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "enabled", defaultValue = "1") val enabled: Boolean = true,
)
