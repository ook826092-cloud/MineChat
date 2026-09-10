package cn.mine.minestars.ui.pages.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.UserPersonaDAO
import cn.mine.minestars.data.db.entity.UserPersonaEntity
import java.util.UUID

sealed interface UserPersonaListEvent {
    data class Success(val message: String) : UserPersonaListEvent
    data class Error(val message: String) : UserPersonaListEvent
}

class UserPersonaListVM(
    private val userPersonaDao: UserPersonaDAO,
) : ViewModel() {
    val personas = userPersonaDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _events = MutableSharedFlow<UserPersonaListEvent>()
    val events = _events.asSharedFlow()

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    fun create(name: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val personaName = name.ifBlank { "未命名" }
            userPersonaDao.insert(
                UserPersonaEntity(
                    id = id,
                    name = personaName,
                    description = "",
                )
            )
            _events.emit(UserPersonaListEvent.Success("用户设定「$personaName」已创建"))
        }
    }

    fun update(id: String, name: String, description: String) {
        viewModelScope.launch {
            userPersonaDao.insert(
                UserPersonaEntity(
                    id = id,
                    name = name,
                    description = description,
                )
            )
            _events.emit(UserPersonaListEvent.Success("用户设定已保存"))
        }
    }

    fun copyPersona(id: String) {
        viewModelScope.launch {
            val original = userPersonaDao.getById(id) ?: return@launch
            val copy = original.copy(
                id = UUID.randomUUID().toString(),
                name = "${original.name} (副本)",
            )
            userPersonaDao.insert(copy)
            _events.emit(UserPersonaListEvent.Success("用户设定「${copy.name}」已创建"))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            userPersonaDao.deleteById(id)
            _events.emit(UserPersonaListEvent.Success("用户设定已删除"))
        }
    }
}
