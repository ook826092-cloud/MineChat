package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Entity(
    tableName = "workspaces",
    indices = [
        Index(value = ["root"], unique = true),
        Index(value = ["updated_at"]),
    ],
)
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val root: String,
    @ColumnInfo(name = "shell_status") val shellStatus: String = "DISABLED",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "last_access_at") val lastAccessAt: Long? = null,
    @ColumnInfo(name = "tool_approvals", defaultValue = "{}")
    val toolApprovals: String = "{}",
) {
    fun toolApprovalOverrides(): Map<String, Boolean> = runCatching {
        val json = Json { ignoreUnknownKeys = true }
        json.decodeFromJsonElement<Map<String, Boolean>>(
            json.parseToJsonElement(toolApprovals).jsonObject
        )
    }.getOrDefault(emptyMap())
}
