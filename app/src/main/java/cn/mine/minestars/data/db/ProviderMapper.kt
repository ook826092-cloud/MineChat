package cn.mine.minestars.data.db

import cn.mine.ai.provider.Model
import cn.mine.ai.provider.ProviderSetting
import cn.mine.minestars.data.db.entity.ProviderEntity
import cn.mine.minestars.utils.JsonInstant
import kotlin.uuid.Uuid

fun ProviderEntity.toProviderSetting(): ProviderSetting {
    return kotlin.runCatching {
        JsonInstant.decodeFromString<ProviderSetting>(this.config)
            .copyProvider(builtIn = this.builtIn)
    }.getOrElse {
        // Stale provider type that was removed — return disabled stub
        ProviderSetting.OpenAI().copy(
            id = kotlin.uuid.Uuid.parse(id),
            enabled = false,
            name = "Unknown (${type})",
            builtIn = builtIn,
        )
    }
}

fun ProviderSetting.toEntity(
    type: String,
    displayOrder: Int,
    builtIn: Boolean,
): ProviderEntity {
    return ProviderEntity(
        id = this.id.toString(),
        name = this.name,
        type = type,
        enabled = this.enabled,
        builtIn = builtIn,
        config = JsonInstant.encodeToString(this),
        displayOrder = displayOrder,
    )
}

fun List<ProviderEntity>.toProviderSettings(): List<ProviderSetting> {
    return map { it.toProviderSetting() }
}

fun List<ProviderSetting>.findModelById(uuid: Uuid): Model? {
    this.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == uuid) return model
        }
    }
    return null
}

fun Model.findProvider(providers: List<ProviderSetting>, checkOverwrite: Boolean = true): ProviderSetting? {
    val provider = findModelProviderFromList(providers) ?: return null
    val providerOverwrite = this.providerOverwrite
    if (checkOverwrite && providerOverwrite != null) {
        return providerOverwrite.copyProvider(models = emptyList())
    }
    return provider
}

private fun Model.findModelProviderFromList(providers: List<ProviderSetting>): ProviderSetting? {
    providers.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == this.id) return setting
        }
    }
    return null
}

fun List<ProviderSetting>.isNotConfigured(): Boolean = all { it.models.isEmpty() }
