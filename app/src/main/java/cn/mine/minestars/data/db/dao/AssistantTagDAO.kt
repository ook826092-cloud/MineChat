package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.AssistantTagEntity

@Dao
interface AssistantTagDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AssistantTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AssistantTagEntity>)

    @Query("SELECT * FROM assistant_tags WHERE assistant_id = :assistantId")
    suspend fun getTagsOfAssistant(assistantId: String): List<AssistantTagEntity>

    @Query("SELECT * FROM assistant_tags WHERE tag_id = :tagId")
    suspend fun getAssistantsOfTag(tagId: String): List<AssistantTagEntity>

    @Query("DELETE FROM assistant_tags WHERE assistant_id = :assistantId AND tag_id = :tagId")
    suspend fun delete(assistantId: String, tagId: String)

    @Query("DELETE FROM assistant_tags WHERE assistant_id = :assistantId")
    suspend fun deleteByAssistant(assistantId: String)

    @Query("DELETE FROM assistant_tags")
    suspend fun deleteAll()
}
