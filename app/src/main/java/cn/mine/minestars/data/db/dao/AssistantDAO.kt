package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.mine.minestars.data.db.entity.AssistantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AssistantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AssistantEntity>)

    @Update
    suspend fun update(item: AssistantEntity)

    @Query("SELECT * FROM assistants ORDER BY display_order ASC")
    suspend fun getAll(): List<AssistantEntity>

    @Query("SELECT * FROM assistants ORDER BY display_order ASC")
    fun getAllFlow(): Flow<List<AssistantEntity>>

    @Query("SELECT * FROM assistants WHERE id = :id")
    suspend fun getById(id: String): AssistantEntity?

    @Query("SELECT * FROM assistants WHERE id = :id")
    fun getByIdFlow(id: String): Flow<AssistantEntity?>

    @Query("DELETE FROM assistants WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM assistants")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM assistants")
    suspend fun count(): Int
}
