package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.ASRProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ASRProviderDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ASRProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ASRProviderEntity>)

    @Query("SELECT * FROM asr_providers ORDER BY name ASC")
    suspend fun getAll(): List<ASRProviderEntity>

    @Query("SELECT * FROM asr_providers ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ASRProviderEntity>>

    @Query("SELECT * FROM asr_providers WHERE id = :id")
    suspend fun getById(id: String): ASRProviderEntity?

    @Query("DELETE FROM asr_providers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM asr_providers")
    suspend fun deleteAll()
}
