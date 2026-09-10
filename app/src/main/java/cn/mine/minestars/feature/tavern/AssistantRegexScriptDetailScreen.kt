package cn.mine.minestars.feature.tavern

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.ui.context.LocalNavController
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

/**
 * Detail page for editing a regex script on an assistant (Layer 2).
 * Thin wrapper around [RegexScriptEditorBody] with AssistantRegexListVM binding.
 */
@Composable
fun AssistantRegexScriptDetailPage(
    assistantId: String,
    scriptId: String,
    listVm: AssistantRegexListVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val scripts by listVm.scripts.collectAsStateWithLifecycle()
    val script = scripts.find { it.id == scriptId }

    val initialRawJson = script?.rawJson ?: return

    RegexScriptEditorBody(
        scriptId = scriptId,
        initialRawJson = initialRawJson,
        scriptTitle = runCatching { JSONObject(initialRawJson).optString("scriptName", "") }.getOrDefault(""),
        onSave = { id, rawJson -> listVm.updateScriptRawJson(id, rawJson) },
        navController = navController,
    )
}
