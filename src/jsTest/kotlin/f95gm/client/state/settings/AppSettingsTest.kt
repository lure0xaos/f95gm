package f95gm.client.state.settings

import f95gm.client.model.settings.*
import f95gm.client.state.runtime.BrowserEffects
import f95gm.client.state.runtime.WindowBrowserEffects
import f95gm.client.state.runtime.BrowserStorage
import f95gm.client.state.runtime.BrowserStorageRuntime
import f95gm.client.state.runtime.BrowserStoragePort
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {
    @Test
    fun persistsAndLoadsAllApplicationSettings() {
        val values = mutableMapOf<String, String>()
        val storage = BrowserStorage(object : BrowserStorageRuntime {
            override val localStorage = object : BrowserStoragePort {
                override fun read(key: String): String? = values[key]
                override fun write(key: String, value: String) {
                    values[key] = value
                }
            }
            override val sessionStorage = localStorage
        })
        val expected = AppSettingsState(
            theme = AppTheme.LIGHT,
            lazyLoadImages = false,
            updateCheckInterval = UpdateCheckInterval.DAY,
            pageSize = PageSize.HUGE,
            cardSize = CardSize.VERY_LARGE
        )

        persistAppSettings(expected, storage)

        assertEquals(expected, loadAppSettings(storage))
    }

    @Test
    fun invalidStoredValuesUseApplicationDefaults() {
        val storage = BrowserStorage(object : BrowserStorageRuntime {
            override val localStorage = object : BrowserStoragePort {
                override fun read(key: String): String? = when (key) {
                    SettingsStorageKeys.LAZY_LOAD_IMAGES.value -> null
                    else -> "invalid"
                }

                override fun write(key: String, value: String) = Unit
            }
            override val sessionStorage = localStorage
        })

        assertEquals(AppSettingsState(), loadAppSettings(storage))
    }

    @Test
    fun systemThemeUsesTheBrowserPreferenceForDesktopRendering() {
        val darkBrowser = object : BrowserEffects by WindowBrowserEffects {
            override fun prefersDarkTheme() = true
        }
        val lightBrowser = object : BrowserEffects by WindowBrowserEffects {
            override fun prefersDarkTheme() = false
        }
        assertEquals("dark", AppTheme.SYSTEM.desktopValue(darkBrowser))
        assertEquals("light", AppTheme.SYSTEM.desktopValue(lightBrowser))
    }

    @Test
    fun individualSettingParsersUseStableDefaultsForUnknownValues() {
        assertEquals(AppTheme.DARK, AppTheme.fromValue("unknown"))
        assertEquals(UpdateCheckInterval.FIFTEEN_MINUTES, UpdateCheckInterval.fromMillis(-1))
        assertEquals(PageSize.LARGE, PageSize.fromValue(999))
        assertEquals(CardSize.DEFAULT, CardSize.fromValue("unknown"))
    }
}
