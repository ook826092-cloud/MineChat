package cn.mine.minestars.ui.pages.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import cn.mine.minestars.data.repository.WorkspaceRepository

class WorkspaceVM(
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {
    val workspaces = workspaceRepository.workspaces
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createWorkspace(name: String) {
        viewModelScope.launch {
            workspaceRepository.createWorkspace(name)
        }
    }

    fun renameWorkspace(id: String, name: String) {
        viewModelScope.launch {
            workspaceRepository.renameWorkspace(id, name)
        }
    }

    fun deleteWorkspace(entity: WorkspaceEntity) {
        viewModelScope.launch {
            workspaceRepository.deleteWorkspace(entity)
        }
    }
}
