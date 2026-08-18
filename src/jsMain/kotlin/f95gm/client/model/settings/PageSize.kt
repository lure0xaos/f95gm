package f95gm.client.model.settings

internal enum class PageSize(val value: Int) {
    TINY(15),
    SMALL(30),
    MEDIUM_SMALL(45),
    MEDIUM(60),
    MEDIUM_LARGE(75),
    LARGE(90),
    EXTRA_LARGE(120),
    HUGE(150);

    companion object {
        fun fromValue(value: Int?): PageSize = entries.firstOrNull { it.value == value } ?: LARGE
    }
}
