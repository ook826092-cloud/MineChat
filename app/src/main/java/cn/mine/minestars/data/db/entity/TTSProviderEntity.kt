package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_providers")
data class TTSProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val config: String,
)
