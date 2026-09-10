package cn.mine.minestars.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.mine.minestars.data.db.entity.CharacterCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterCardDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CharacterCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CharacterCardEntity>)

    @Query("SELECT * FROM character_cards ORDER BY name ASC")
    suspend fun getAll(): List<CharacterCardEntity>

    @Query("SELECT * FROM character_cards ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CharacterCardEntity>>

    @Query("SELECT * FROM character_cards WHERE id = :id")
    suspend fun getById(id: String): CharacterCardEntity?

    @Query("DELETE FROM character_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM character_cards")
    suspend fun deleteAll()
}
