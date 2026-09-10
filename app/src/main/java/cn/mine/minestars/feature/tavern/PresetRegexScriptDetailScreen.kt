package cn.mine.minestars.feature.tavern

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.core.tavern.regex.RegexScript
import cn.mine.minestars.ui.context.LocalNavController
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

/**
 * Detail page for editing a regex script in a preset (stored in extensions.regex_scripts).
 * Thin wrapper around [RegexScriptEditorBody] with PresetDetailVM binding.
 */
@Composable
fun PresetRegexScriptDetailPage(
    presetId: String,
    scriptId: String,
    vm: PresetDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val modelParams by vm.modelParams.collectAsStateWithLifecycle()
    val script = modelParams.regexScripts.find { it.id == scriptId } ?: return

    RegexScriptEditorBody(
        scriptId = scriptId,
        initialRawJson = script.toJson().toString(),
        scriptTitle = script.scriptName,
        onSave = { id, rawJson ->
            RegexScript.fromJson(JSONObject(rawJson))?.let {
                vm.updateRegexScript(id, it.copy(origin = "preset"))
            }
        },
        navController = navController,
    )
}
