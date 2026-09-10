package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.WorldBookDAO
import cn.mine.minestars.data.import.CharacterImportService
import java.util.UUID

sealed interface WorldBookListEvent {
    data class Success(val message: String) : WorldBookListEvent
    data class Error(val message: String) : WorldBookListEvent
}

class WorldBookListVM(
    private val worldBookDao: WorldBookDAO,
) : ViewModel() {
    val worldBooks = worldBookDao.getAllGlobalFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _events = MutableSharedFlow<WorldBookListEvent>()
    val events = _events.asSharedFlow()

    private val _importing = MutableStateFlow(false)
    val importing = _importing.asStateFlow()

    fun createWorldBook() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            worldBookDao.insert(
                cn.mine.minestars.data.db.entity.WorldBookEntity(
                    id = id,
                    name = "新建世界书",
                    rawJson = """{"name":"新建世界书"}"""
                )
            )
            _events.emit(WorldBookListEvent.Success("世界书已创建"))
        }
    }

    fun importWorldBook(resolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            _importing.value = true
            try {
                val count = CharacterImportService.importWorldBooks(resolver, uri)
                _events.emit(WorldBookListEvent.Success("导入了 $count 个世界书"))
            } catch (e: Exception) {
                android.util.Log.e("WorldBookImport", "导入世界书失败", e)
                _events.emit(WorldBookListEvent.Error("导入失败: ${e.message}"))
            } finally {
                _importing.value = false
            }
        }
    }

    fun copyWorldBook(id: String) {
        viewModelScope.launch {
            val book = worldBookDao.getById(id) ?: return@launch
            val newId = UUID.randomUUID().toString()
            val newName = "${book.name} (副本)"
            val newJson = org.json.JSONObject(book.rawJson).apply {
                put("name", newName)
            }.toString()
            worldBookDao.insert(book.copy(
                id = newId,
                name = newName,
                rawJson = newJson,
            ))
            _events.emit(WorldBookListEvent.Success("世界书已复制"))
        }
    }

    fun deleteWorldBook(id: String) {
        viewModelScope.launch {
            worldBookDao.deleteById(id)
        }
    }
}
