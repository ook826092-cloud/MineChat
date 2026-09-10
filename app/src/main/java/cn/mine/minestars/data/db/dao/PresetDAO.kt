package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PresetEntity>)

    @Query("SELECT * FROM presets ORDER BY name ASC")
    suspend fun getAll(): List<PresetEntity>

    @Query("SELECT * FROM presets ORDER BY name ASC")
    fun getAllFlow(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: String): PresetEntity?

    @Query("SELECT * FROM presets WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultPreset(): PresetEntity?

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM presets")
    suspend fun deleteAll()
}
