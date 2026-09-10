package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.WorldBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldBookDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorldBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorldBookEntity>)

    @Query("SELECT * FROM world_books ORDER BY name ASC")
    suspend fun getAll(): List<WorldBookEntity>

    @Query("SELECT * FROM world_books ORDER BY name ASC")
    fun getAllFlow(): Flow<List<WorldBookEntity>>

    @Query("SELECT * FROM world_books ORDER BY name ASC")
    fun getAllGlobalFlow(): Flow<List<WorldBookEntity>>

    @Query("SELECT * FROM world_books WHERE id = :id")
    suspend fun getById(id: String): WorldBookEntity?

    @Query("DELETE FROM world_books WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM world_books")
    suspend fun deleteAll()
}
