package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.WorldBookEntryEntity

@Dao
interface WorldBookEntryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorldBookEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorldBookEntryEntity>)

    @Query("SELECT * FROM world_book_entries WHERE world_book_id = :worldBookId ORDER BY `order` ASC")
    suspend fun getEntriesOfWorldBook(worldBookId: String): List<WorldBookEntryEntity>

    @Query("DELETE FROM world_book_entries WHERE world_book_id = :worldBookId AND entry_id = :entryId")
    suspend fun delete(worldBookId: String, entryId: String)

    @Query("DELETE FROM world_book_entries WHERE world_book_id = :worldBookId")
    suspend fun deleteByWorldBook(worldBookId: String)

    @Query("DELETE FROM world_book_entries")
    suspend fun deleteAll()
}
