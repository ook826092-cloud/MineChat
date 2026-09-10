package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regex_scripts")
data class RegexScriptEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "scope") val scope: String,
    @ColumnInfo(name = "group_id") val groupId: String?,
)
