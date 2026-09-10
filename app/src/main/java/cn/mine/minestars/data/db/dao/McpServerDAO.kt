package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.McpServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface McpServerDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: McpServerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<McpServerEntity>)

    @Query("SELECT * FROM mcp_servers ORDER BY display_order ASC")
    suspend fun getAll(): List<McpServerEntity>

    @Query("SELECT * FROM mcp_servers ORDER BY display_order ASC")
    fun getAllFlow(): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE id = :id")
    suspend fun getById(id: String): McpServerEntity?

    @Query("SELECT * FROM mcp_servers WHERE enabled = 1 ORDER BY display_order ASC")
    suspend fun getEnabled(): List<McpServerEntity>

    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM mcp_servers")
    suspend fun deleteAll()
}
