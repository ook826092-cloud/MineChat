package cn.mine.minestars.ui.pages.imggen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import cn.mine.ai.provider.ImageEditParams
import cn.mine.ai.provider.ImageGenerationParams
import cn.mine.ai.provider.ProviderManager
import cn.mine.ai.ui.ImageAspectRatio
import cn.mine.ai.ui.ImageGenerationItem
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.findModelById
import cn.mine.minestars.data.db.findProvider
import cn.mine.minestars.data.db.toEntity
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.entity.GenMediaEntity
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.data.repository.GenMediaRepository
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

@Serializable
data class GeneratedImage(
    val id: Int,
    val prompt: String,
    val filePath: String,
    val timestamp: Long,
    val model: String
)

private fun GenMediaEntity.toGeneratedImage(filesManager: FilesManager): GeneratedImage {
    val imagesDir = filesManager.getImagesDir()
    val fullPath = File(imagesDir, this.path.removePrefix("images/")).absolutePath

    return GeneratedImage(
        id = this.id,
        prompt = this.prompt,
        filePath = fullPath,
        timestamp = this.createAt,
        model = this.modelId
    )
}

class ImgGenVM(
    context: Application,
    val settingsStore: SettingsStore,
    val providerManager: ProviderManager,
    val genMediaRepository: GenMediaRepository,
    private val filesManager: FilesManager,
    private val providerDAO: ProviderDAO,
    private val modelSelectionDAO: ModelSelectionDAO,
) : AndroidViewModel(context) {
    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    private var cancelJob: Job? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentGeneratedImages = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val currentGeneratedImages: StateFlow<List<GeneratedImage>> = _currentGeneratedImages

    private val _referenceImages = MutableStateFlow<List<String>>(emptyList())
    val referenceImages: StateFlow<List<String>> = _referenceImages

    val pager = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { genMediaRepository.getAllMedia() }
    )
    val generatedImages: Flow<PagingData<GeneratedImage>> = pager.flow
        .map { pagingData ->
            pagingData.map { entity -> entity.toGeneratedImage(filesManager) }
        }
        .cachedIn(viewModelScope)

    fun updatePrompt(prompt: String) {
        _prompt.value = prompt
    }

    fun addReferenceImages(paths: List<String>) {
        _referenceImages.value = (_referenceImages.value + paths).distinct().take(MAX_REFERENCE_IMAGES)
    }

    fun removeReferenceImage(path: String) {
        _referenceImages.value = _referenceImages.value.filterNot { it == path }
        deleteReferenceFiles(listOf(path))
    }

    fun clearReferenceImages() {
        deleteReferenceFiles(_referenceImages.value)
        _referenceImages.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    fun startNewSession() {
        cancelJob?.cancel()
        clearReferenceImages()
        _prompt.value = ""
        _currentGeneratedImages.value = emptyList()
        _error.value = null
        _isGenerating.value = false
    }

    fun generateImage() {
        if(prompt.value.isBlank()) return
        cancelJob?.cancel()
        cancelJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null
                _currentGeneratedImages.value = emptyList()

                val allProviders = providerDAO.getAll().toProviderSettings()
                val imageGenModelId = modelSelectionDAO.get()?.imageGenerationModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() }
                val model = imageGenModelId?.let { allProviders.findModelById(it) }
                    ?: throw IllegalStateException("No model selected")

                val provider = model.findProvider(allProviders)
                    ?: throw IllegalStateException("Provider not found")

                val providerSetting = allProviders.find { it.id == provider.id }
                    ?: throw IllegalStateException("Provider setting not found")

                val imageCount = model.customParams["num_images"]?.toIntOrNull()?.coerceIn(1, 4) ?: 1
                val resolvedAspectRatio = when (model.customParams["aspect_ratio"]) {
                    "LANDSCAPE" -> ImageAspectRatio.LANDSCAPE
                    "PORTRAIT" -> ImageAspectRatio.PORTRAIT
                    else -> ImageAspectRatio.SQUARE
                }

                val params = ImageGenerationParams(
                    model = model,
                    prompt = _prompt.value,
                    numOfImages = imageCount,
                    aspectRatio = resolvedAspectRatio,
                    customParams = model.customParams,
                    customHeaders = model.customHeaders,
                    customBody = model.customBodies,
                )

                val newImages = MutableList<GeneratedImage?>(imageCount) { null }

                providerManager.getProviderByType(provider)
                    .generateImage(providerSetting, params)
                    .collect { item ->
                        if (item.partial) {
                            val index = item.partialImageIndex ?: return@collect
                            val imagesDir = filesManager.getImagesDir()
                            val previewFile = java.io.File(imagesDir, "preview_${index}_${System.currentTimeMillis()}.png")
                            filesManager.createImageFileFromBase64(item.data, previewFile.absolutePath)
                            val previewImage = GeneratedImage(
                                id = -1,
                                prompt = _prompt.value,
                                filePath = previewFile.absolutePath,
                                timestamp = System.currentTimeMillis(),
                                model = model.displayName
                            )
                            newImages[index] = previewImage
                            _currentGeneratedImages.value = newImages.filterNotNull()
                        } else {
                            val index = item.partialImageIndex ?: (newImages.indexOfFirst { it == null }.takeIf { it >= 0 } ?: newImages.size)
                            val imageFile = saveImageToStorage(
                                item = item,
                                prompt = _prompt.value,
                                modelName = model.displayName,
                                index = index
                            )
                            val generatedImage = GeneratedImage(
                                id = 0,
                                prompt = _prompt.value,
                                filePath = imageFile.absolutePath,
                                timestamp = System.currentTimeMillis(),
                                model = model.displayName
                            )
                            if (index < newImages.size) {
                                newImages[index] = generatedImage
                            } else {
                                newImages.add(generatedImage)
                            }
                            _currentGeneratedImages.value = newImages.filterNotNull()
                        }
                    }
            } catch (e: Exception) {
                if(e is CancellationException) return@launch
                Log.e(TAG, "Failed to generate image", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun editImage() {
        if (prompt.value.isBlank() || referenceImages.value.isEmpty()) return
        cancelJob?.cancel()
        cancelJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null
                _currentGeneratedImages.value = emptyList()

                val allProviders = providerDAO.getAll().toProviderSettings()
                val imageGenModelId = modelSelectionDAO.get()?.imageGenerationModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() }
                val model = imageGenModelId?.let { allProviders.findModelById(it) }
                    ?: throw IllegalStateException("No model selected")

                val provider = model.findProvider(allProviders)
                    ?: throw IllegalStateException("Provider not found")

                val providerSetting = allProviders.find { it.id == provider.id }
                    ?: throw IllegalStateException("Provider setting not found")

                val sourceImages = _referenceImages.value
                val editImageCount = model.customParams["num_images"]?.toIntOrNull()?.coerceIn(1, 4) ?: 1
                val editAspectRatio = when (model.customParams["aspect_ratio"]) {
                    "LANDSCAPE" -> ImageAspectRatio.LANDSCAPE
                    "PORTRAIT" -> ImageAspectRatio.PORTRAIT
                    else -> ImageAspectRatio.SQUARE
                }
                val params = ImageEditParams(
                    model = model,
                    prompt = _prompt.value,
                    images = sourceImages,
                    numOfImages = editImageCount,
                    aspectRatio = editAspectRatio,
                    customHeaders = model.customHeaders,
                    customBody = model.customBodies,
                )

                val newImages = MutableList<GeneratedImage?>(editImageCount) { null }

                providerManager.getProviderByType(provider)
                    .editImage(providerSetting, params)
                    .collect { item ->
                        if (item.partial) {
                            val index = item.partialImageIndex ?: return@collect
                            val imagesDir = filesManager.getImagesDir()
                            val previewFile = java.io.File(imagesDir, "preview_${index}_${System.currentTimeMillis()}.png")
                            filesManager.createImageFileFromBase64(item.data, previewFile.absolutePath)
                            val previewImage = GeneratedImage(
                                id = -1,
                                prompt = _prompt.value,
                                filePath = previewFile.absolutePath,
                                timestamp = System.currentTimeMillis(),
                                model = model.displayName
                            )
                            newImages[index] = previewImage
                            _currentGeneratedImages.value = newImages.filterNotNull()
                        } else {
                            val index = item.partialImageIndex ?: (newImages.indexOfFirst { it == null }.takeIf { it >= 0 } ?: newImages.size)
                            val imageFile = saveImageToStorage(
                                item = item,
                                prompt = _prompt.value,
                                modelName = model.displayName,
                                index = index,
                                type = GenMediaEntity.TYPE_IMAGE_EDIT,
                            )
                            val generatedImage = GeneratedImage(
                                id = 0,
                                prompt = _prompt.value,
                                filePath = imageFile.absolutePath,
                                timestamp = System.currentTimeMillis(),
                                model = model.displayName
                            )
                            if (index < newImages.size) {
                                newImages[index] = generatedImage
                            } else {
                                newImages.add(generatedImage)
                            }
                            _currentGeneratedImages.value = newImages.filterNotNull()
                        }
                    }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                Log.e(TAG, "Failed to edit image", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun cancelGeneration() {
        cancelJob?.cancel()
    }

    fun updateModelCustomParams(modelId: Uuid, customParams: Map<String, String>) {
        viewModelScope.launch {
            val allProviders = providerDAO.getAll().toProviderSettings()
            val model = allProviders.findModelById(modelId) ?: return@launch
            val provider = model.findProvider(allProviders, checkOverwrite = false) ?: return@launch

            val updatedModel = model.copy(customParams = customParams)
            val updatedProvider = provider.editModel(updatedModel)

            val oldEntity = providerDAO.getById(updatedProvider.id.toString()) ?: return@launch
            providerDAO.insert(updatedProvider.toEntity(
                type = oldEntity.type,
                displayOrder = oldEntity.displayOrder,
                builtIn = oldEntity.builtIn,
            ))
        }
    }

    private suspend fun saveImageToStorage(
        item: ImageGenerationItem,
        prompt: String,
        modelName: String,
        index: Int,
        type: String = GenMediaEntity.TYPE_IMAGE_GENERATION,
        sourcePaths: String? = null,
    ): File {
        val imagesDir = filesManager.getImagesDir()

        val timestamp = System.currentTimeMillis()
        val filename = "${timestamp}_${modelName}_$index.png"
        val imageFile = File(imagesDir, filename)

        val createdFile = filesManager.createImageFileFromBase64(item.data, imageFile.absolutePath)

        // Save to database with relative path
        val relativePath = "images/${imageFile.name}"
        val entity = GenMediaEntity(
            path = relativePath,
            modelId = modelName,
            prompt = prompt,
            createAt = timestamp,
            type = type,
            sourcePaths = sourcePaths,
        )
        genMediaRepository.insertMedia(entity)

        return createdFile
    }

    fun deleteImage(image: GeneratedImage) {
        viewModelScope.launch {
            try {
                // Delete from database first
                genMediaRepository.deleteMedia(image.id)

                // Then delete the file
                val file = File(image.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete image", e)
                _error.value = "Failed to delete image"
            }
        }
    }

    private fun deleteReferenceFiles(paths: List<String>) {
        viewModelScope.launch {
            paths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    companion object {
        private const val TAG = "ImgGenVM"
        private const val MAX_REFERENCE_IMAGES = 16
    }
}
