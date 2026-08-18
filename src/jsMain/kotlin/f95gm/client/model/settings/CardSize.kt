package f95gm.client.model.settings

internal enum class CardSize(val value: String, val gridClass: String) {
    VERY_COMPACT("very-compact", "row-cols-1 row-cols-sm-4 row-cols-lg-5 row-cols-xxl-6"),
    COMPACT("compact", "row-cols-1 row-cols-sm-3 row-cols-lg-4 row-cols-xxl-6"),
    DEFAULT("default", "row-cols-1 row-cols-sm-2 row-cols-lg-3 row-cols-xxl-4"),
    LARGE("large", "row-cols-1 row-cols-sm-1 row-cols-lg-2 row-cols-xxl-3"),
    VERY_LARGE("very-large", "row-cols-1 row-cols-sm-1 row-cols-lg-1 row-cols-xxl-2");

    companion object {
        fun fromValue(value: String?): CardSize = entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}
