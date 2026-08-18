package f95gm.client.state.settings

import f95gm.client.model.settings.*
import kotlinx.browser.window

internal fun loadAppSettings(): AppSettingsState {
    return runCatching {
        val storage = window.localStorage
        AppSettingsState(
            theme = AppTheme.fromValue(storage.getItem(SettingsStorageKeys.THEME)),
            lazyLoadImages = storage.getItem(SettingsStorageKeys.LAZY_LOAD_IMAGES)?.equals("true", ignoreCase = true)
                ?: true,
            updateCheckInterval = UpdateCheckInterval.fromMillis(
                storage.getItem(SettingsStorageKeys.UPDATE_CHECK_INTERVAL)?.toLongOrNull()
            ),
            pageSize = PageSize.fromValue(storage.getItem(SettingsStorageKeys.PAGE_SIZE)?.toIntOrNull()),
            cardSize = CardSize.fromValue(storage.getItem(SettingsStorageKeys.CARD_SIZE))
        )
    }.getOrDefault(AppSettingsState())
}
