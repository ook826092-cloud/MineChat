package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.KnowledgeBaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeBaseDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: KnowledgeBaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KnowledgeBaseEntity>)

    @Query("SELECT * FROM knowledge_bases ORDER BY name ASC")
    suspend fun getAll(): List<KnowledgeBaseEntity>

    @Query("SELECT * FROM knowledge_bases ORDER BY name ASC")
    fun getAllFlow(): Flow<List<KnowledgeBaseEntity>>

    @Query("SELECT * FROM knowledge_bases WHERE id = :id")
    suspend fun getById(id: String): KnowledgeBaseEntity?

    @Query("DELETE FROM knowledge_bases WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM knowledge_bases")
    suspend fun deleteAll()
}
