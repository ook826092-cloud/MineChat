package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "assistant_mcp_servers", primaryKeys = ["assistant_id", "mcp_server_id"])
data class AssistantMcpServerEntity(
    @ColumnInfo(name = "assistant_id") val assistantId: String,
    @ColumnInfo(name = "mcp_server_id") val mcpServerId: String,
)
