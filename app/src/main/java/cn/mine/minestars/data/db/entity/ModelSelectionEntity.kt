package cn.mine.minestars.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_selections")
data class ModelSelectionEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "chat_model_id") val chatModelId: String? = null,
    @ColumnInfo(name = "fast_model_id") val fastModelId: String? = null,
    @ColumnInfo(name = "title_model_id") val titleModelId: String? = null,
    @ColumnInfo(name = "image_generation_model_id") val imageGenerationModelId: String? = null,
    @ColumnInfo(name = "translate_model_id") val translateModelId: String? = null,
    @ColumnInfo(name = "suggestion_model_id") val suggestionModelId: String? = null,
    @ColumnInfo(name = "ocr_model_id") val ocrModelId: String? = null,
    @ColumnInfo(name = "compress_model_id") val compressModelId: String? = null,
)
