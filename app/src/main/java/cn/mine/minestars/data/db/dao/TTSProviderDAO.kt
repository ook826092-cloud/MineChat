package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.TTSProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TTSProviderDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TTSProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TTSProviderEntity>)

    @Query("SELECT * FROM tts_providers ORDER BY name ASC")
    suspend fun getAll(): List<TTSProviderEntity>

    @Query("SELECT * FROM tts_providers ORDER BY name ASC")
    fun getAllFlow(): Flow<List<TTSProviderEntity>>

    @Query("SELECT * FROM tts_providers WHERE id = :id")
    suspend fun getById(id: String): TTSProviderEntity?

    @Query("DELETE FROM tts_providers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tts_providers")
    suspend fun deleteAll()
}
