package f95gm.client.state.settings

import f95gm.client.model.settings.AppSettingsState
import f95gm.client.state.runtime.BrowserStorage
internal fun persistAppSettings(settings: AppSettingsState, storage: BrowserStorage) {
    storage.writeBestEffort(SettingsStorageKeys.THEME, SettingsStorageCodec.encode(settings.theme))
    storage.writeBestEffort(SettingsStorageKeys.LAZY_LOAD_IMAGES, SettingsStorageCodec.encode(settings.lazyLoadImages))
    storage.writeBestEffort(SettingsStorageKeys.UPDATE_CHECK_INTERVAL, SettingsStorageCodec.encode(settings.updateCheckInterval))
    storage.writeBestEffort(SettingsStorageKeys.PAGE_SIZE, SettingsStorageCodec.encode(settings.pageSize))
    storage.writeBestEffort(SettingsStorageKeys.CARD_SIZE, SettingsStorageCodec.encode(settings.cardSize))
}
