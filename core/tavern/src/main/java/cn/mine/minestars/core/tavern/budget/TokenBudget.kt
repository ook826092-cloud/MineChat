package cn.mine.minestars.core.tavern.budget

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.EncodingType

object TokenBudgetEstimator {
    private val enc = Encodings.newDefaultEncodingRegistry()

    fun count(text: String): Int {
        if (text.isBlank()) return 0
        return runCatching {
            val encoding = enc.getEncoding(EncodingType.CL100K_BASE)
            encoding.encode(text).size
        }.getOrElse {
            (text.length / 4.0).toInt().coerceAtLeast(1)
        }
    }
}
