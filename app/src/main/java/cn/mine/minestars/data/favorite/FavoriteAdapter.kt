package cn.mine.minestars.data.favorite

import cn.mine.minestars.data.db.entity.FavoriteEntity
import cn.mine.minestars.data.model.FavoriteType

interface FavoriteAdapter<T> {
    val type: FavoriteType

    fun buildRefKey(target: T): String

    fun buildFavoriteEntity(
        target: T,
        existing: FavoriteEntity? = null,
        now: Long = System.currentTimeMillis()
    ): FavoriteEntity
}
