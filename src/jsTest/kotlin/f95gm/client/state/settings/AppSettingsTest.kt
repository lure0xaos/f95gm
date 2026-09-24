package f95gm.client.state.settings

import f95gm.client.model.settings.*
import f95gm.client.state.runtime.BrowserRuntime
import f95gm.client.state.runtime.BrowserRuntimePort
import f95gm.client.state.runtime.BrowserStorage
import f95gm.client.state.runtime.BrowserStoragePort
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {
    @Test
    fun persistsAndLoadsAllApplicationSettings() {
        val previous = BrowserStorage.localPort
        try {
            val values = mutableMapOf<String, String>()
            BrowserStorage.localPort = object : BrowserStoragePort {
                override fun read(key: String): String? = values[key]
                override fun write(key: String, value: String) {
                    values[key] = value
                }
            }
            val expected = AppSettingsState(
                theme = AppTheme.LIGHT,
                lazyLoadImages = false,
                updateCheckInterval = UpdateCheckInterval.DAY,
                pageSize = PageSize.HUGE,
                cardSize = CardSize.VERY_LARGE
            )

            persistAppSettings(expected)

            assertEquals(expected, loadAppSettings())
        } finally {
            BrowserStorage.localPort = previous
        }
    }

    @Test
    fun invalidStoredValuesUseApplicationDefaults() {
        val previous = BrowserStorage.localPort
        try {
            BrowserStorage.localPort = object : BrowserStoragePort {
                override fun read(key: String): String? = when (key) {
                    SettingsStorageKeys.LAZY_LOAD_IMAGES -> null
                    else -> "invalid"
                }

                override fun write(key: String, value: String) = Unit
            }

            assertEquals(AppSettingsState(), loadAppSettings())
        } finally {
            BrowserStorage.localPort = previous
        }
    }

    @Test
    fun systemThemeUsesTheBrowserPreferenceForDesktopRendering() {
        val previous = BrowserRuntime.port
        try {
            BrowserRuntime.port = object : BrowserRuntimePort {
                override val pathname = "/"
                override val search = ""
                override fun prefersDarkTheme() = true
            }
            assertEquals("dark", AppTheme.SYSTEM.desktopValue())

            BrowserRuntime.port = object : BrowserRuntimePort {
                override val pathname = "/"
                override val search = ""
                override fun prefersDarkTheme() = false
            }
            assertEquals("light", AppTheme.SYSTEM.desktopValue())
        } finally {
            BrowserRuntime.port = previous
        }
    }

    @Test
    fun individualSettingParsersUseStableDefaultsForUnknownValues() {
        assertEquals(AppTheme.DARK, AppTheme.fromValue("unknown"))
        assertEquals(UpdateCheckInterval.FIFTEEN_MINUTES, UpdateCheckInterval.fromMillis(-1))
        assertEquals(PageSize.LARGE, PageSize.fromValue(999))
        assertEquals(CardSize.DEFAULT, CardSize.fromValue("unknown"))
    }
}
