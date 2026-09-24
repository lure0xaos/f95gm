package f95gm.client.state.settings

import f95gm.client.model.settings.*
import f95gm.client.state.runtime.BrowserStorage

internal fun loadAppSettings(): AppSettingsState {
    return AppSettingsState(
        theme = AppTheme.fromValue(BrowserStorage.read(SettingsStorageKeys.THEME)),
        lazyLoadImages = BrowserStorage.read(SettingsStorageKeys.LAZY_LOAD_IMAGES)?.equals("true", ignoreCase = true)
            ?: true,
        updateCheckInterval = UpdateCheckInterval.fromMillis(
            BrowserStorage.read(SettingsStorageKeys.UPDATE_CHECK_INTERVAL)?.toLongOrNull()
        ),
        pageSize = PageSize.fromValue(BrowserStorage.read(SettingsStorageKeys.PAGE_SIZE)?.toIntOrNull()),
        cardSize = CardSize.fromValue(BrowserStorage.read(SettingsStorageKeys.CARD_SIZE))
    )
}
