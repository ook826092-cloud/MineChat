package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProviderEntity>)

    @Query("SELECT * FROM providers ORDER BY display_order ASC")
    suspend fun getAll(): List<ProviderEntity>

    @Query("SELECT * FROM providers ORDER BY display_order ASC")
    fun getAllFlow(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: String): ProviderEntity?

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM providers")
    suspend fun deleteAll()
}
