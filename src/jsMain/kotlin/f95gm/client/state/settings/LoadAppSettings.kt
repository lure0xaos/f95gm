package f95gm.client.state.settings

import f95gm.client.model.settings.*
import f95gm.client.state.runtime.BrowserStorage

internal fun loadAppSettings(storage: BrowserStorage): AppSettingsState {
    return AppSettingsState(
        theme = SettingsStorageCodec.decodeTheme(storage.readOrNull(SettingsStorageKeys.THEME)),
        lazyLoadImages = SettingsStorageCodec.decodeBoolean(storage.readOrNull(SettingsStorageKeys.LAZY_LOAD_IMAGES), true),
        updateCheckInterval = SettingsStorageCodec.decodeUpdateCheckInterval(
            storage.readOrNull(SettingsStorageKeys.UPDATE_CHECK_INTERVAL)
        ),
        pageSize = SettingsStorageCodec.decodePageSize(storage.readOrNull(SettingsStorageKeys.PAGE_SIZE)),
        cardSize = SettingsStorageCodec.decodeCardSize(storage.readOrNull(SettingsStorageKeys.CARD_SIZE))
    )
}
