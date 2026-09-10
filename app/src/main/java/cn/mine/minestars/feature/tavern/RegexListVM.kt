package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.RegexGroupDAO
import cn.mine.minestars.data.db.dao.RegexScriptDAO
import cn.mine.minestars.data.db.entity.RegexGroupEntity
import cn.mine.minestars.data.import.CharacterImportService
import org.json.JSONArray
import java.util.UUID

sealed interface RegexListEvent {
    data class Success(val message: String) : RegexListEvent
    data class Error(val message: String) : RegexListEvent
}

data class GroupWithStats(
    val group: RegexGroupEntity,
    val scriptCount: Int,
)

class RegexListVM(
    private val regexGroupDao: RegexGroupDAO,
    private val regexScriptDao: RegexScriptDAO,
) : ViewModel() {
    private val _events = MutableSharedFlow<RegexListEvent>()
    val events = _events.asSharedFlow()

    private val _importing = MutableStateFlow(false)
    val importing = _importing.asStateFlow()

    /** Groups with their script counts, reactive. */
    val groupsWithStats = combine(
        regexGroupDao.getAllFlow(),
        regexScriptDao.getAllFlow(),
    ) { groups, scripts ->
        groups.map { group ->
            val scriptIds = parseJsonArray(group.regexIdsJson)
            GroupWithStats(
                group = group,
                scriptCount = scriptIds.count { id -> scripts.any { it.id == id } },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** All scripts (for migration / orphan detection). */
    val allScripts = regexScriptDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Check if migration is needed: scripts exist but no groups. */
    suspend fun needsMigration(): Boolean {
        val groups = regexGroupDao.getAll()
        val scripts = regexScriptDao.getAll()
        return groups.isEmpty() && scripts.isNotEmpty()
    }

    /** Migrate: collect all existing scripts into a default group. */
    fun migrateLegacyScripts() {
        viewModelScope.launch {
            val scripts = regexScriptDao.getAll()
            if (scripts.isEmpty()) return@launch
            val existingGroups = regexGroupDao.getAll()
            if (existingGroups.isNotEmpty()) return@launch

            val groupId = UUID.randomUUID().toString()
            val scriptIds = JSONArray(scripts.map { it.id }).toString()
            regexGroupDao.insert(
                RegexGroupEntity(
                    id = groupId,
                    name = "默认组",
                    characterName = null,
                    regexIdsJson = scriptIds,
                    scope = "IMPORTED",
                )
            )
            _events.emit(RegexListEvent.Success("已将 ${scripts.size} 个正则迁移到「默认组」"))
        }
    }

    fun createGroup(name: String = "新建正则组") {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            regexGroupDao.insert(
                RegexGroupEntity(
                    id = id,
                    name = name,
                    characterName = null,
                    regexIdsJson = "[]",
                    scope = "IMPORTED",
                )
            )
            _events.emit(RegexListEvent.Success("正则组已创建"))
        }
    }

    fun renameGroup(id: String, newName: String) {
        viewModelScope.launch {
            val group = regexGroupDao.getById(id) ?: return@launch
            regexGroupDao.update(group.copy(name = newName))
            _events.emit(RegexListEvent.Success("已重命名"))
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            regexGroupDao.deleteById(id)
            _events.emit(RegexListEvent.Success("正则组已删除"))
        }
    }

    fun importRegex(resolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            _importing.value = true
            try {
                val count = CharacterImportService.importRegexScripts(resolver, uri)
                _events.emit(RegexListEvent.Success("导入了 $count 个正则"))
            } catch (e: Exception) {
                android.util.Log.e("RegexImport", "导入正则失败", e)
                _events.emit(RegexListEvent.Error("导入失败: ${e.message}"))
            } finally {
                _importing.value = false
            }
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
