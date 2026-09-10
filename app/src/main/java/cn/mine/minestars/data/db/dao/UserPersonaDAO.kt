package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.UserPersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPersonaDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UserPersonaEntity)

    @Query("SELECT * FROM user_personas ORDER BY created_at ASC")
    suspend fun getAll(): List<UserPersonaEntity>

    @Query("SELECT * FROM user_personas ORDER BY created_at ASC")
    fun getAllFlow(): Flow<List<UserPersonaEntity>>

    @Query("SELECT * FROM user_personas WHERE id = :id")
    suspend fun getById(id: String): UserPersonaEntity?

    @Query("DELETE FROM user_personas WHERE id = :id")
    suspend fun deleteById(id: String)
}
