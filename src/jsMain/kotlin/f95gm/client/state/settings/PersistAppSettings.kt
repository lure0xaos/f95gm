package f95gm.client.state.settings

import f95gm.client.model.settings.AppSettingsState
import f95gm.client.state.runtime.BrowserStorage

internal fun persistAppSettings(settings: AppSettingsState) {
    BrowserStorage.write(SettingsStorageKeys.THEME, settings.theme.bootstrapValue)
    BrowserStorage.write(SettingsStorageKeys.LAZY_LOAD_IMAGES, settings.lazyLoadImages.toString())
    BrowserStorage.write(SettingsStorageKeys.UPDATE_CHECK_INTERVAL, settings.updateCheckInterval.millis.toString())
    BrowserStorage.write(SettingsStorageKeys.PAGE_SIZE, settings.pageSize.value.toString())
    BrowserStorage.write(SettingsStorageKeys.CARD_SIZE, settings.cardSize.value)
}
