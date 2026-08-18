package f95gm.domain.tracking

import kotlinx.serialization.Serializable

@Serializable
enum class GameTrackingState(val wireValue: String) {
    WATCHING("watching"),
    DOWNLOADING("downloading"),
    DOWNLOADED("downloaded"),
    PLAYABLE("playable"),
    PAUSED("paused"),
    COMPLETED("completed");

    companion object {
        fun fromWire(value: String): GameTrackingState? =
            entries.firstOrNull { it.wireValue == value.lowercase() }
    }
}
