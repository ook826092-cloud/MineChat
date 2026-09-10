package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asr_providers")
data class ASRProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val config: String,
)
