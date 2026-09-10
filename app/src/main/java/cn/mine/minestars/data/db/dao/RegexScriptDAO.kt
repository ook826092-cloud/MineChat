package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.mine.minestars.data.db.entity.RegexScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexScriptDAO {
    // ── regex_scripts (Layer 1/3) ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RegexScriptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RegexScriptEntity>)

    @Update
    suspend fun update(item: RegexScriptEntity)

    @Query("SELECT * FROM regex_scripts ORDER BY name ASC")
    suspend fun getAll(): List<RegexScriptEntity>

    @Query("SELECT * FROM regex_scripts ORDER BY name ASC")
    fun getAllFlow(): Flow<List<RegexScriptEntity>>

    @Query("SELECT * FROM regex_scripts WHERE id = :id")
    suspend fun getById(id: String): RegexScriptEntity?

    @Query("SELECT * FROM regex_scripts WHERE group_id = :groupId ORDER BY name ASC")
    suspend fun getByGroupId(groupId: String): List<RegexScriptEntity>

    @Query("DELETE FROM regex_scripts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM regex_scripts")
    suspend fun deleteAll()
}
