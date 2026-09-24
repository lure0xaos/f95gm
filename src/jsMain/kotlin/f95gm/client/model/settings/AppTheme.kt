package f95gm.client.model.settings

import f95gm.client.state.runtime.BrowserRuntime

internal enum class AppTheme(val bootstrapValue: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("auto");

    fun desktopValue(): String = when (this) {
        DARK -> DARK.bootstrapValue
        LIGHT -> LIGHT.bootstrapValue
        SYSTEM -> if (BrowserRuntime.prefersDarkTheme()) {
            DARK.bootstrapValue
        } else {
            LIGHT.bootstrapValue
        }
    }

    companion object {
        fun fromValue(value: String?): AppTheme = entries.firstOrNull { it.bootstrapValue == value } ?: DARK
    }
}
