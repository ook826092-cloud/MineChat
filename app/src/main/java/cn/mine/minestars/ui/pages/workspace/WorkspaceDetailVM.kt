package cn.mine.minestars.ui.pages.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.mine.minestars.core.workspace.RootfsInstallProgress
import cn.mine.minestars.core.workspace.RootfsInstallStage
import cn.mine.minestars.core.workspace.WorkspaceFileEntry
import cn.mine.minestars.core.workspace.WorkspaceShellStatus
import cn.mine.minestars.core.workspace.WorkspaceStorageArea
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import cn.mine.minestars.data.repository.WorkspaceRepository
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class WorkspaceDetailVM(
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {
    private val _workspace = MutableStateFlow<WorkspaceEntity?>(null)
    val workspace: StateFlow<WorkspaceEntity?> = _workspace.asStateFlow()

    private val _files = MutableStateFlow<List<WorkspaceFileEntry>>(emptyList())
    val files: StateFlow<List<WorkspaceFileEntry>> = _files.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _area = MutableStateFlow(WorkspaceStorageArea.FILES)
    val area: StateFlow<WorkspaceStorageArea> = _area.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _installProgress = MutableStateFlow<RootfsInstallProgress?>(null)
    val installProgress: StateFlow<RootfsInstallProgress?> = _installProgress.asStateFlow()

    private var workspaceRoot: String = ""
    private var workspaceId: String = ""
    private var loadedId: String = ""

    fun loadWorkspace(id: String) {
        if (id == loadedId) return
        loadedId = id
        viewModelScope.launch {
            val entity = workspaceRepository.getWorkspace(id) ?: return@launch
            workspaceId = id
            workspaceRoot = entity.root
            _workspace.value = entity
            refreshFiles()
        }
    }

    fun selectArea(area: WorkspaceStorageArea) {
        _area.value = area
        _currentPath.value = ""
        refreshFiles()
    }

    fun toggleShell(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val status = if (enabled) {
                val ws = workspaceRepository.getWorkspace(id)
                if (ws != null && workspaceRepository.hasRootfs(ws.root)) {
                    WorkspaceShellStatus.READY
                } else {
                    WorkspaceShellStatus.INSTALLING
                }
            } else {
                WorkspaceShellStatus.DISABLED
            }
            workspaceRepository.updateShellStatus(id, status)
            reloadWorkspace()
        }
    }

    fun setToolApproval(id: String, toolName: String, needsApproval: Boolean) {
        viewModelScope.launch {
            workspaceRepository.setToolApproval(id, toolName, needsApproval)
            reloadWorkspace()
        }
    }

    fun installRootfs(root: String, url: String) {
        viewModelScope.launch {
            val ws = _workspace.value ?: return@launch
            _installProgress.value = RootfsInstallProgress(stage = RootfsInstallStage.DOWNLOADING)
            try {
                workspaceRepository.updateShellStatus(ws.id, WorkspaceShellStatus.INSTALLING)
                reloadWorkspace()
                withContext(Dispatchers.IO) {
                    workspaceRepository.installRootfs(root, url) { progress ->
                        _installProgress.value = progress
                    }
                }
                workspaceRepository.updateShellStatus(ws.id, WorkspaceShellStatus.READY)
                reloadWorkspace()
            } catch (e: Exception) {
                workspaceRepository.updateShellStatus(ws.id, WorkspaceShellStatus.DISABLED)
            } finally {
                _installProgress.value = null
            }
        }
    }

    fun importFile(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    workspaceRepository.importFile(
                        root = workspaceRoot,
                        area = _area.value,
                        destinationPath = _currentPath.value,
                        fileName = fileName,
                        inputStream = inputStream,
                    )
                }
            }.onSuccess {
                refreshFiles()
            }.onFailure { e ->
                _error.value = e.message ?: "导入文件失败"
            }
        }
    }

    fun exportFile(entry: WorkspaceFileEntry, outputStream: OutputStream) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    workspaceRepository.exportFile(
                        root = workspaceRoot,
                        area = _area.value,
                        path = entry.path,
                        outputStream = outputStream,
                    )
                }
            }.onFailure { e ->
                _error.value = e.message ?: "导出文件失败"
            }
        }
    }

    fun shareFile(entry: WorkspaceFileEntry, cacheDir: File, onReady: (File) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "workspace_share").apply { mkdirs() }
                    val file = File(dir, entry.name)
                    file.outputStream().use { output ->
                        workspaceRepository.exportFile(
                            root = workspaceRoot,
                            area = _area.value,
                            path = entry.path,
                            outputStream = output,
                        )
                    }
                    file
                }
            }.onSuccess(onReady).onFailure { e ->
                _error.value = e.message ?: "分享文件失败"
            }
        }
    }

    fun deleteFile(entry: WorkspaceFileEntry) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    workspaceRepository.deleteFile(
                        root = workspaceRoot,
                        path = entry.path,
                        recursive = entry.isDirectory,
                        area = _area.value,
                    )
                }
            }.onSuccess {
                refreshFiles()
            }.onFailure { e ->
                _error.value = e.message ?: "删除文件失败"
            }
        }
    }

    fun resolveFile(root: String, relativePath: String): File =
        workspaceRepository.resolveWorkspaceFile(root, relativePath)

    fun navigateTo(root: String, path: String) {
        _currentPath.value = path
        refreshFiles()
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current.isNotBlank()) {
            _currentPath.value = current.substringBeforeLast('/', missingDelimiterValue = "")
            refreshFiles()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        refreshFiles()
        reloadWorkspace()
    }

    fun exportToCacheFile(entry: WorkspaceFileEntry, cacheDir: File, onReady: (File) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "workspace_export").apply { mkdirs() }
                    val file = File(dir, entry.name)
                    file.outputStream().use { output ->
                        workspaceRepository.exportFile(
                            root = workspaceRoot,
                            area = _area.value,
                            path = entry.path,
                            outputStream = output,
                        )
                    }
                    file
                }
            }.onSuccess(onReady).onFailure { e ->
                _error.value = e.message ?: "导出文件失败"
            }
        }
    }

    private fun refreshFiles() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _files.value = withContext(Dispatchers.IO) {
                    workspaceRepository.listFiles(workspaceRoot, _area.value, _currentPath.value)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "加载文件失败"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun reloadWorkspace() {
        viewModelScope.launch {
            _workspace.value = workspaceRepository.getWorkspace(workspaceId)
        }
    }
}
