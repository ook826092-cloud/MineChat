package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.SearchServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchServiceDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SearchServiceEntity>)

    @Query("SELECT * FROM search_services")
    suspend fun getAll(): List<SearchServiceEntity>

    @Query("SELECT * FROM search_services")
    fun getAllFlow(): Flow<List<SearchServiceEntity>>

    @Query("SELECT * FROM search_services WHERE id = :id")
    suspend fun getById(id: String): SearchServiceEntity?

    @Query("DELETE FROM search_services WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM search_services")
    suspend fun deleteAll()
}
