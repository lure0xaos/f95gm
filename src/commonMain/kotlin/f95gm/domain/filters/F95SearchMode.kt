package f95gm.domain.filters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class F95SearchMode(val wireValue: String) {
    @SerialName("title")
    TITLE("title"),

    @SerialName("creator")
    CREATOR("creator");

    companion object {
        fun fromWire(value: String): F95SearchMode = entries.firstOrNull { it.wireValue == value.lowercase() } ?: TITLE
    }
}
