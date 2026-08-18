package f95gm.client.model.settings

import kotlinx.browser.window

internal enum class AppTheme(val bootstrapValue: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("auto");

    fun desktopValue(): String = when (this) {
        DARK -> DARK.bootstrapValue
        LIGHT -> LIGHT.bootstrapValue
        SYSTEM -> if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
            DARK.bootstrapValue
        } else {
            LIGHT.bootstrapValue
        }
    }

    companion object {
        fun fromValue(value: String?): AppTheme = entries.firstOrNull { it.bootstrapValue == value } ?: DARK
    }
}
