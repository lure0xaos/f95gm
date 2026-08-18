package f95gm.domain.filters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class F95Sort(val wireValue: String) {
    @SerialName("date")
    DATE("date"),

    @SerialName("date_asc")
    DATE_ASC("date_asc"),

    @SerialName("likes")
    LIKES("likes"),

    @SerialName("likes_asc")
    LIKES_ASC("likes_asc"),

    @SerialName("views")
    VIEWS("views"),

    @SerialName("views_asc")
    VIEWS_ASC("views_asc"),

    @SerialName("title")
    TITLE("title"),

    @SerialName("title_desc")
    TITLE_DESC("title_desc"),

    @SerialName("rating")
    RATING("rating"),

    @SerialName("rating_asc")
    RATING_ASC("rating_asc");

    companion object {
        fun fromWire(value: String): F95Sort = entries.firstOrNull { it.wireValue == value.lowercase() } ?: DATE
    }
}
