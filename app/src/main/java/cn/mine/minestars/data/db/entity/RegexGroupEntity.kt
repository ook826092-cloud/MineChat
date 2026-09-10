package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regex_groups")
data class RegexGroupEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "character_name") val characterName: String?,
    @ColumnInfo(name = "regex_ids_json") val regexIdsJson: String,
    @ColumnInfo(name = "scope") val scope: String,
)
