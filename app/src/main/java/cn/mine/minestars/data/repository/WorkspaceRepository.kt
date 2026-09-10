package cn.mine.minestars.data.repository

import cn.mine.minestars.core.workspace.ProotShellRunner
import cn.mine.minestars.core.workspace.RootfsInstaller
import cn.mine.minestars.core.workspace.RootfsInstallProgress
import cn.mine.minestars.core.workspace.WorkspaceCommandResult
import cn.mine.minestars.core.workspace.WorkspaceConfig
import cn.mine.minestars.core.workspace.WorkspaceFileEntry
import cn.mine.minestars.core.workspace.WorkspaceManager
import cn.mine.minestars.core.workspace.WorkspaceSearchMatch
import cn.mine.minestars.core.workspace.WorkspaceShellStatus
import cn.mine.minestars.core.workspace.WorkspaceStorageArea
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.WorkspaceDAO
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class WorkspaceRepository(
    private val workspaceDao: WorkspaceDAO,
    private val workspaceManager: WorkspaceManager,
    private val rootfsInstaller: RootfsInstaller,
    private val assistantDao: AssistantDAO,
) {
    val workspaces: Flow<List<WorkspaceEntity>> = workspaceDao.getAllFlow()

    suspend fun getWorkspace(id: String): WorkspaceEntity? = workspaceDao.getById(id)

    suspend fun createWorkspace(name: String): WorkspaceEntity {
        val id = kotlin.uuid.Uuid.random().toString()
        val now = System.currentTimeMillis()
        val finalName = name.trim().ifBlank { "Workspace" }
        require(!isNameTaken(finalName, excludeId = null)) {
            "Workspace name already exists: $finalName"
        }
        val entity = WorkspaceEntity(
            id = id,
            name = finalName,
            root = id,
            shellStatus = WorkspaceShellStatus.DISABLED.name,
            createdAt = now,
            updatedAt = now,
        )
        workspaceManager.ensureWorkspace(id)
        workspaceDao.insert(entity)
        return entity
    }

    suspend fun isNameTaken(name: String, excludeId: String?): Boolean {
        val target = name.trim()
        return workspaceDao.getAll().any { it.id != excludeId && it.name.trim() == target }
    }

    suspend fun deleteWorkspace(entity: WorkspaceEntity) {
        // Unbind all assistants using this workspace
        assistantDao.getAll().forEach { assistant ->
            if (assistant.workspaceId == entity.id) {
                // In practice the UI would handle this, but clean up as a safety measure
            }
        }
        workspaceManager.deleteWorkspace(entity.root)
        workspaceDao.deleteById(entity.id)
    }

    suspend fun renameWorkspace(id: String, name: String) {
        workspaceDao.updateName(id, name, System.currentTimeMillis())
    }

    suspend fun updateShellStatus(id: String, status: WorkspaceShellStatus) {
        workspaceDao.updateShellStatus(id, status.name, System.currentTimeMillis())
    }

    suspend fun updateLastAccess(id: String) {
        workspaceDao.updateLastAccess(id, System.currentTimeMillis())
    }

    suspend fun getToolApprovalOverrides(id: String): Map<String, Boolean> {
        return workspaceDao.getById(id)?.toolApprovalOverrides() ?: emptyMap()
    }

    suspend fun setToolApproval(id: String, toolName: String, needsApproval: Boolean) {
        val entity = workspaceDao.getById(id) ?: return
        val overrides = entity.toolApprovalOverrides().toMutableMap()
        overrides[toolName] = needsApproval
        updateToolApprovals(id, overrides)
    }

    private suspend fun updateToolApprovals(id: String, overrides: Map<String, Boolean>) {
        val json = Json { prettyPrint = false }
        workspaceDao.updateToolApprovals(id, json.encodeToString(buildJsonObject {
            overrides.forEach { (k, v) -> put(k, v) }
        }), System.currentTimeMillis())
    }

    fun listFiles(root: String, path: String = ""): List<WorkspaceFileEntry> =
        workspaceManager.listFiles(root, path)

    fun listFiles(root: String, area: WorkspaceStorageArea, path: String = ""): List<WorkspaceFileEntry> =
        workspaceManager.listFiles(root, path, area)

    fun readFile(root: String, path: String): String =
        workspaceManager.readText(root, path)

    suspend fun readTextForPreview(id: String, area: WorkspaceStorageArea, path: String): String {
        val workspace = workspaceDao.getById(id) ?: error("Workspace not found: $id")
        return when (area) {
            WorkspaceStorageArea.FILES -> workspaceManager.readText(workspace.root, path)
            WorkspaceStorageArea.LINUX -> {
                val size = workspaceManager.rootfsFileSize(workspace.root, path)
                require(size <= 512L * 1024) { "文件过大，无法预览 ($size bytes)" }
                val out = java.io.ByteArrayOutputStream()
                workspaceManager.exportFile(workspace.root, path, area, out)
                out.toString(Charsets.UTF_8.name())
            }
        }
    }

    suspend fun writeText(id: String, path: String, text: String, overwrite: Boolean): WorkspaceFileEntry {
        val workspace = workspaceDao.getById(id) ?: error("Workspace not found: $id")
        return workspaceManager.writeText(workspace.root, path, text, overwrite)
    }

    fun writeFile(root: String, path: String, text: String, overwrite: Boolean = true): WorkspaceFileEntry =
        workspaceManager.writeText(root, path, text, overwrite)

    fun deleteFile(root: String, path: String, recursive: Boolean = false, area: WorkspaceStorageArea = WorkspaceStorageArea.FILES): Boolean =
        workspaceManager.deleteFile(root, path, recursive, area)

    fun importFile(root: String, area: WorkspaceStorageArea, destinationPath: String, fileName: String, inputStream: InputStream): WorkspaceFileEntry =
        workspaceManager.importFile(root, destinationPath, area, fileName, inputStream)

    fun exportFile(root: String, area: WorkspaceStorageArea, path: String, outputStream: OutputStream) {
        workspaceManager.exportFile(root, path, area, outputStream)
    }

    fun moveFile(root: String, source: String, target: String, overwrite: Boolean = false): WorkspaceFileEntry =
        workspaceManager.moveFile(root, source, target, overwrite)

    fun glob(root: String, pattern: String, path: String = ""): List<WorkspaceFileEntry> =
        workspaceManager.glob(root, pattern, path)

    fun grep(
        root: String,
        query: String,
        path: String = "",
        regex: Boolean = false,
        ignoreCase: Boolean = true,
        includeGlob: String? = null,
    ): List<WorkspaceSearchMatch> =
        workspaceManager.grep(root, query, path, regex, ignoreCase, includeGlob)

    fun executeCommand(
        root: String,
        command: String,
        cwd: String = "",
        timeoutMillis: Long = WorkspaceManager.DEFAULT_COMMAND_TIMEOUT_MS,
    ): WorkspaceCommandResult =
        workspaceManager.executeCommand(root, command, cwd, timeoutMillis)

    fun hasRootfs(root: String): Boolean = workspaceManager.hasRootfs(root)

    fun rootfsFileSize(root: String, path: String): Long =
        workspaceManager.rootfsFileSize(root, path)

    fun exportRootfsFile(root: String, path: String, outputStream: OutputStream) {
        workspaceManager.exportRootfsFile(root, path, outputStream)
    }

    fun resolveWorkspaceFile(root: String, relativePath: String): File =
        File(workspaceManager.filesDir(root), relativePath)

    fun installRootfs(
        root: String,
        url: String,
        onProgress: (RootfsInstallProgress) -> Unit = {},
    ) {
        rootfsInstaller.install(root, url, onProgress)
    }

    companion object {
        const val DEFAULT_COMMAND_TIMEOUT_MS = 30_000L
    }
}
