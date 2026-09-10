package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_messages")
data class QuickMessageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)
