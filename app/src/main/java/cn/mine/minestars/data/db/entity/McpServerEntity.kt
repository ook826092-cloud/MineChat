package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    val config: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)
