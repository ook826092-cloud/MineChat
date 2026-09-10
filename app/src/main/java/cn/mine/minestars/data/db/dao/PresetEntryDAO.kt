package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.mine.minestars.data.db.entity.PresetEntryEntity

@Dao
interface PresetEntryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PresetEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PresetEntryEntity>)

    @Update
    suspend fun update(item: PresetEntryEntity)

    @Query("SELECT * FROM preset_entries WHERE preset_id = :presetId ORDER BY entry_index ASC")
    suspend fun getByPresetId(presetId: String): List<PresetEntryEntity>

    @Query("DELETE FROM preset_entries WHERE preset_id = :presetId AND entry_index = :entryIndex")
    suspend fun delete(presetId: String, entryIndex: Int)

    @Query("DELETE FROM preset_entries WHERE preset_id = :presetId")
    suspend fun deleteByPresetId(presetId: String)

    @Query("DELETE FROM preset_entries")
    suspend fun deleteAll()
}
