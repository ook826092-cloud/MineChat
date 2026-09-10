package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.ModelSelectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelSelectionDAO {
    @Query("SELECT * FROM model_selections WHERE id = 1")
    fun getFlow(): Flow<ModelSelectionEntity?>

    @Query("SELECT * FROM model_selections WHERE id = 1")
    suspend fun get(): ModelSelectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: ModelSelectionEntity)
}
