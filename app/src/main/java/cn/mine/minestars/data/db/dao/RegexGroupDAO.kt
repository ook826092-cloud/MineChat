package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.mine.minestars.data.db.entity.RegexGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexGroupDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RegexGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RegexGroupEntity>)

    @Update
    suspend fun update(item: RegexGroupEntity)

    @Query("SELECT * FROM regex_groups ORDER BY name ASC")
    suspend fun getAll(): List<RegexGroupEntity>

    @Query("SELECT * FROM regex_groups ORDER BY name ASC")
    fun getAllFlow(): Flow<List<RegexGroupEntity>>

    @Query("SELECT * FROM regex_groups WHERE id = :id")
    suspend fun getById(id: String): RegexGroupEntity?

    @Query("DELETE FROM regex_groups WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM regex_groups")
    suspend fun deleteAll()
}
