package cn.mine.ai.provider

import kotlinx.serialization.Serializable

/**
 * Defines a parameter that an image generation model supports.
 */
@Serializable
data class ParamDef(
    val name: String,
    val type: ParamType = ParamType.STRING,
    val defaultValue: String = "",
    val description: String = "",
    val required: Boolean = false,
    val minimum: Double? = null,
    val maximum: Double? = null,
)

@Serializable
enum class ParamType(val jsonType: String) {
    STRING("string"),
    INT("integer"),
    FLOAT("number"),
    BOOL("boolean");
}
