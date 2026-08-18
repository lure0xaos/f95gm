package f95gm.client.state.settings

import f95gm.client.model.settings.AppSettingsState
import kotlinx.browser.window

internal fun persistAppSettings(settings: AppSettingsState) {
    runCatching {
        val storage = window.localStorage
        storage.setItem(SettingsStorageKeys.THEME, settings.theme.bootstrapValue)
        storage.setItem(SettingsStorageKeys.LAZY_LOAD_IMAGES, settings.lazyLoadImages.toString())
        storage.setItem(SettingsStorageKeys.UPDATE_CHECK_INTERVAL, settings.updateCheckInterval.millis.toString())
        storage.setItem(SettingsStorageKeys.PAGE_SIZE, settings.pageSize.value.toString())
        storage.setItem(SettingsStorageKeys.CARD_SIZE, settings.cardSize.value)
    }
}
