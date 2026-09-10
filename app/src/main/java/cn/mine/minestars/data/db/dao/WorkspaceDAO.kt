package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDAO {
    @Query("SELECT * FROM workspaces ORDER BY updated_at DESC")
    fun getAllFlow(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces ORDER BY updated_at DESC")
    suspend fun getAll(): List<WorkspaceEntity>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getById(id: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE workspaces SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: String, name: String, updatedAt: Long)

    @Query("UPDATE workspaces SET shell_status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateShellStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE workspaces SET tool_approvals = :toolApprovals, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateToolApprovals(id: String, toolApprovals: String, updatedAt: Long)

    @Query("UPDATE workspaces SET last_access_at = :lastAccessAt WHERE id = :id")
    suspend fun updateLastAccess(id: String, lastAccessAt: Long)
}
