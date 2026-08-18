package f95gm.domain.filters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class F95TagType(val wireValue: String) {
    @SerialName("or")
    OR("or"),

    @SerialName("and")
    AND("and");

    companion object {
        fun fromWire(value: String): F95TagType = entries.firstOrNull { it.wireValue == value.lowercase() } ?: OR
    }
}
