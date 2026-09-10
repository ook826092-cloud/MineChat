package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_services")
data class SearchServiceEntity(
    @PrimaryKey val id: String,
    val type: String,
    val config: String,
)
