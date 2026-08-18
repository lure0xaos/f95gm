package f95gm.domain.filters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class F95Category(val wireValue: String) {
    @SerialName("games")
    GAMES("games"),

    @SerialName("comics")
    COMICS("comics"),

    @SerialName("animations")
    ANIMATIONS("animations"),

    @SerialName("assets")
    ASSETS("assets"),

    @SerialName("mods")
    MODS("mods");

    companion object {
        fun fromWire(value: String): F95Category = entries.firstOrNull { it.wireValue == value.lowercase() } ?: GAMES
    }
}
