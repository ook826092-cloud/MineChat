package cn.mine.minestars.rag.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmbeddingDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmbeddingEntity>)

    @Query("SELECT * FROM embeddings WHERE knowledge_base_id = :kbId")
    suspend fun getByKnowledgeBaseId(kbId: String): List<EmbeddingEntity>

    @Query("DELETE FROM embeddings WHERE knowledge_base_id = :kbId")
    suspend fun deleteByKnowledgeBaseId(kbId: String)

    @Query("DELETE FROM embeddings WHERE knowledge_base_id = :kbId AND file_uri = :fileUri")
    suspend fun deleteByFileUri(kbId: String, fileUri: String)

    @Query("SELECT DISTINCT file_uri FROM embeddings WHERE knowledge_base_id = :kbId")
    suspend fun getIndexedFileUris(kbId: String): List<String>

    @Query("SELECT COUNT(*) FROM embeddings WHERE knowledge_base_id = :kbId AND file_uri = :fileUri")
    suspend fun getChunkCount(kbId: String, fileUri: String): Int
}
