package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_cards")
data class CharacterCardEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "source_file_name") val sourceFileName: String,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "linked_world_book_ids_json") val linkedWorldBookIdsJson: String,
    @ColumnInfo(name = "linked_regex_ids_json") val linkedRegexIdsJson: String,
    @ColumnInfo(name = "linked_regex_group_id") val linkedRegexGroupId: String?,
)
