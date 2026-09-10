package cn.mine.minestars.data.repository

import androidx.paging.PagingSource
import cn.mine.minestars.data.db.dao.GenMediaDAO
import cn.mine.minestars.data.db.entity.GenMediaEntity

class GenMediaRepository(private val dao: GenMediaDAO) {
    fun getAllMedia(): PagingSource<Int, GenMediaEntity> = dao.getAll()

    suspend fun insertMedia(media: GenMediaEntity) = dao.insert(media)

    suspend fun deleteMedia(id: Int) = dao.delete(id)
}
