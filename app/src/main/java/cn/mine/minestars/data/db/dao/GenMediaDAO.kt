package cn.mine.minestars.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.mine.minestars.data.db.entity.GenMediaEntity

@Dao
interface GenMediaDAO {
    @Query("SELECT * FROM gen_media ORDER BY create_at DESC")
    fun getAll(): PagingSource<Int, GenMediaEntity>

    @Query("SELECT * FROM gen_media ORDER BY create_at DESC")
    suspend fun getAllMedia(): List<GenMediaEntity>

    @Insert
    suspend fun insert(media: GenMediaEntity)

    @Query("DELETE FROM gen_media WHERE id = :id")
    suspend fun delete(id: Int)
}
