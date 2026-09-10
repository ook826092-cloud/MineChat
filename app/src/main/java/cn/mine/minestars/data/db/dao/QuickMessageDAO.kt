package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.QuickMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickMessageDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QuickMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuickMessageEntity>)

    @Query("SELECT * FROM quick_messages ORDER BY display_order ASC")
    suspend fun getAll(): List<QuickMessageEntity>

    @Query("SELECT * FROM quick_messages ORDER BY display_order ASC")
    fun getAllFlow(): Flow<List<QuickMessageEntity>>

    @Query("SELECT * FROM quick_messages WHERE id = :id")
    suspend fun getById(id: String): QuickMessageEntity?

    @Query("DELETE FROM quick_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM quick_messages")
    suspend fun deleteAll()
}
