package cn.mine.minestars.feature.character

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.CharacterCardDAO
import cn.mine.minestars.data.import.CharacterImportService
import cn.mine.minestars.data.import.CharacterImportResult

class CharacterCardVM(
    private val characterCardDao: CharacterCardDAO,
) : ViewModel() {
    val characterCards = characterCardDao
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _importing = MutableStateFlow(false)
    val importing = _importing.asStateFlow()

    private val _events = MutableSharedFlow<CharacterCardEvent>()
    val events = _events.asSharedFlow()

    fun importCharacterCard(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _events.emit(
                runCatching {
                    CharacterImportService.importCharacterCard(resolver, uri)
                }.fold(
                    onSuccess = { result ->
                        CharacterCardEvent.ImportSuccess(result)
                    },
                    onFailure = { error ->
                        CharacterCardEvent.ImportError(error.message ?: "Import failed")
                    }
                )
            )
            _importing.value = false
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            characterCardDao.deleteById(id)
        }
    }
}

sealed interface CharacterCardEvent {
    data class ImportSuccess(val result: CharacterImportResult) : CharacterCardEvent
    data class ImportError(val message: String) : CharacterCardEvent
}
