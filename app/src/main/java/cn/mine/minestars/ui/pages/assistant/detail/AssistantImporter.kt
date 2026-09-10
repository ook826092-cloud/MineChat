package cn.mine.minestars.ui.pages.assistant.detail

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.model.Avatar
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.data.import.CharacterImportResult
import cn.mine.minestars.data.import.CharacterImportService
import cn.mine.minestars.ui.components.ui.AutoAIIcon
import cn.mine.minestars.ui.context.LocalToaster
import cn.mine.minestars.R
import org.koin.compose.koinInject
import java.util.UUID
import kotlin.uuid.Uuid

@Composable
fun AssistantImporter(
    modifier: Modifier = Modifier,
    onUpdate: (Assistant) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        SillyTavernImporter(onImport = onUpdate)
    }
}

@Composable
private fun SillyTavernImporter(
    onImport: (Assistant) -> Unit
) {
    val context = LocalContext.current
    val filesManager: FilesManager = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    var isLoading by remember { mutableStateOf(false) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    runCatching {
                        importAssistantFromUri(
                            context = context,
                            uri = uri,
                            onImport = onImport,
                            toaster = toaster,
                            filesManager = filesManager,
                        )
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        toaster.show(exception.message ?: context.getString(R.string.assistant_importer_import_failed))
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val pngPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    runCatching {
                        importAssistantFromUri(
                            context = context,
                            uri = uri,
                            onImport = onImport,
                            toaster = toaster,
                            filesManager = filesManager,
                        )
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        toaster.show(exception.message ?: context.getString(R.string.assistant_importer_import_failed))
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                pngPickerLauncher.launch(arrayOf("image/png"))
            },
            enabled = !isLoading
        ) {
            AutoAIIcon(name = "tavern", modifier = Modifier.padding(end = 8.dp))
            Text(text = if (isLoading) stringResource(R.string.assistant_importer_importing) else "角色卡 Png")
        }

        OutlinedButton(
            onClick = {
                jsonPickerLauncher.launch(arrayOf("application/json"))
            },
            enabled = !isLoading
        ) {
            AutoAIIcon(name = "tavern", modifier = Modifier.padding(end = 8.dp))
            Text(text = if (isLoading) stringResource(R.string.assistant_importer_importing) else "角色卡 Json")
        }
    }
}

private suspend fun importAssistantFromUri(
    context: Context,
    uri: Uri,
    onImport: (Assistant) -> Unit,
    toaster: ToasterState,
    filesManager: FilesManager,
) {
    try {
        // Generate assistant ID BEFORE import so Layer 2 items (world books, regex)
        // can be stored with the assistantId FK right away.
        val assistantId = UUID.randomUUID().toString()

        val importResult = withContext(Dispatchers.IO) {
            CharacterImportService.importCharacterCard(
                resolver = context.contentResolver,
                uri = uri,
                assistantId = assistantId,
            )
        }
        val avatar = withContext(Dispatchers.IO) {
            importResult.avatarUri?.let { avatarUri ->
                filesManager.createChatFilesByContents(listOf(avatarUri.toUri()))
                    .firstOrNull()
                    ?.toString()
            }
        }
        val assistant = importResult.toAssistant(
            avatarUri = avatar,
            assistantId = assistantId,
        )
        onImport(assistant)
    } catch (exception: Exception) {
        exception.printStackTrace()
        toaster.show(
            message = exception.message ?: context.getString(R.string.assistant_importer_import_failed),
            type = ToastType.Error
        )
    }
}

private fun CharacterImportResult.toAssistant(
    avatarUri: String?,
    assistantId: String,
): Assistant {
    return Assistant(
        id = runCatching { kotlin.uuid.Uuid.parse(assistantId) }.getOrNull() ?: kotlin.uuid.Uuid.random(),
        name = fields.name,
        charName = fields.name,
        description = fields.description,
        personality = fields.personality,
        scenario = fields.scenario,
        firstMessage = fields.firstMessage,
        mesExamples = fields.mesExamples,
        mainPromptOverride = fields.systemPrompt,
        postHistoryInstructions = fields.postHistoryInstructions,
        creatorNotes = fields.creatorNotes,
        alternateGreetings = fields.alternateGreetings,
        groupOnlyGreetings = fields.groupOnlyGreetings,
        nickname = fields.nickname,
        cardTags = fields.tags,
        creator = fields.creator,
        characterVersion = fields.characterVersion,
        source = fields.source,
        // Layer 2 items are stored as embedded JSON on the assistant
        worldBookIds = emptySet(),
        regexGroupIds = emptySet(),
        worldBookJson = worldBookJson,
        regexScriptsJson = regexScriptsJson,
        avatar = avatarUri?.let { Avatar.Image(it) } ?: Avatar.Dummy,
        useAssistantAvatar = avatarUri != null,
    )
}
