package cn.mine.minestars.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Earth
import me.rerere.hugeicons.stroke.FileZip
import me.rerere.hugeicons.stroke.MessageMultiple01
import me.rerere.hugeicons.stroke.Notebook01
import me.rerere.hugeicons.stroke.View
import cn.mine.ai.core.ReasoningLevel
import cn.mine.minestars.R
import cn.mine.minestars.data.ai.prompts.DEFAULT_COMPRESS_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_OCR_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_SUGGESTION_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_TITLE_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_TRANSLATION_PROMPT
import cn.mine.minestars.data.datastore.AiSettings
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.ui.components.ai.ReasoningButton
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus

@Composable
internal fun PromptSettingsPage(settings: Settings, aiSettings: AiSettings, vm: SettingVM, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PromptCard(
                icon = { Icon(HugeIcons.Earth, null) },
                title = { Text(stringResource(R.string.setting_model_page_prompt_translation), maxLines = 1) },
            ) {
                PromptTextField(
                    label = stringResource(R.string.setting_model_page_prompt),
                    description = stringResource(R.string.setting_model_page_translate_prompt_vars),
                    value = aiSettings.translatePrompt,
                    onValueChange = { vm.updateAiSettings(aiSettings.copy(translatePrompt = it)) },
                    onReset = { vm.updateAiSettings(aiSettings.copy(translatePrompt = DEFAULT_TRANSLATION_PROMPT)) },
                    reasoningLevel = ReasoningLevel.fromBudgetTokens(aiSettings.translateThinkingBudget),
                    onUpdateReasoningLevel = { vm.updateAiSettings(aiSettings.copy(translateThinkingBudget = it.budgetTokens)) },
                )
            }
        }
        item {
            PromptCard(
                icon = { Icon(HugeIcons.Notebook01, null) },
                title = { Text(stringResource(R.string.setting_model_page_prompt_title), maxLines = 1) },
            ) {
                PromptTextField(
                    label = stringResource(R.string.setting_model_page_prompt),
                    description = stringResource(R.string.setting_model_page_suggestion_prompt_vars),
                    value = aiSettings.titlePrompt,
                    onValueChange = { vm.updateAiSettings(aiSettings.copy(titlePrompt = it)) },
                    onReset = { vm.updateAiSettings(aiSettings.copy(titlePrompt = DEFAULT_TITLE_PROMPT)) },
                )
            }
        }
        item {
            PromptCard(
                icon = { Icon(HugeIcons.MessageMultiple01, null) },
                title = { Text(stringResource(R.string.setting_model_page_prompt_suggestion), maxLines = 1) },
            ) {
                PromptTextField(
                    label = stringResource(R.string.setting_model_page_prompt),
                    description = stringResource(R.string.setting_model_page_suggestion_prompt_vars),
                    value = aiSettings.suggestionPrompt,
                    onValueChange = { vm.updateAiSettings(aiSettings.copy(suggestionPrompt = it)) },
                    onReset = { vm.updateAiSettings(aiSettings.copy(suggestionPrompt = DEFAULT_SUGGESTION_PROMPT)) },
                )
            }
        }
        item {
            PromptCard(
                icon = { Icon(HugeIcons.View, null) },
                title = { Text(stringResource(R.string.setting_model_page_prompt_ocr), maxLines = 1) },
            ) {
                PromptTextField(
                    label = stringResource(R.string.setting_model_page_prompt),
                    description = stringResource(R.string.setting_model_page_ocr_prompt_vars),
                    value = aiSettings.ocrPrompt,
                    onValueChange = { vm.updateAiSettings(aiSettings.copy(ocrPrompt = it)) },
                    onReset = { vm.updateAiSettings(aiSettings.copy(ocrPrompt = DEFAULT_OCR_PROMPT)) },
                )
            }
        }
        item {
            PromptCard(
                icon = { Icon(HugeIcons.FileZip, null) },
                title = { Text(stringResource(R.string.setting_model_page_prompt_compress), maxLines = 1) },
            ) {
                PromptTextField(
                    label = stringResource(R.string.setting_model_page_prompt),
                    description = stringResource(R.string.setting_model_page_compress_prompt_vars),
                    value = aiSettings.compressPrompt,
                    onValueChange = { vm.updateAiSettings(aiSettings.copy(compressPrompt = it)) },
                    onReset = { vm.updateAiSettings(aiSettings.copy(compressPrompt = DEFAULT_COMPRESS_PROMPT)) },
                )
            }
        }
    }
}

@Composable
private fun PromptCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CustomColors.listItemColors.containerColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    title()
                }
            }
            content()
        }
    }
}

@Composable
private fun ColumnScope.PromptTextField(
    label: String,
    description: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
    reasoningLevel: ReasoningLevel? = null,
    onUpdateReasoningLevel: ((ReasoningLevel) -> Unit)? = null,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
    )
    if (description != null) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 10,
    )
    TextButton(onClick = onReset) {
        Text(stringResource(R.string.setting_model_page_reset_to_default))
    }
    if (reasoningLevel != null && onUpdateReasoningLevel != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.assistant_page_thinking_budget))
            ReasoningButton(
                reasoningLevel = reasoningLevel,
                onUpdateReasoningLevel = onUpdateReasoningLevel,
            )
        }
    }
}
