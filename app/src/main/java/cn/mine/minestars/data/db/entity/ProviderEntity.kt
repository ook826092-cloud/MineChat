package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    @ColumnInfo(name = "built_in") val builtIn: Boolean,
    @ColumnInfo(name = "config") val config: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)
