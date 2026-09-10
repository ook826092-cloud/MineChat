package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.AssistantMcpServerEntity

@Dao
interface AssistantMcpServerDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AssistantMcpServerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AssistantMcpServerEntity>)

    @Query("SELECT * FROM assistant_mcp_servers WHERE assistant_id = :assistantId")
    suspend fun getServersOfAssistant(assistantId: String): List<AssistantMcpServerEntity>

    @Query("DELETE FROM assistant_mcp_servers WHERE assistant_id = :assistantId AND mcp_server_id = :mcpServerId")
    suspend fun delete(assistantId: String, mcpServerId: String)

    @Query("DELETE FROM assistant_mcp_servers WHERE assistant_id = :assistantId")
    suspend fun deleteByAssistant(assistantId: String)

    @Query("DELETE FROM assistant_mcp_servers")
    suspend fun deleteAll()
}
