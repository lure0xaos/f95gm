package f95gm.client.state.settings

import f95gm.client.state.runtime.StorageKey

internal object SettingsStorageKeys {
    val THEME = StorageKey("f95gm.settings.theme")
    val LAZY_LOAD_IMAGES = StorageKey("f95gm.settings.lazyLoadImages")
    val UPDATE_CHECK_INTERVAL = StorageKey("f95gm.settings.updateCheckInterval")
    val PAGE_SIZE = StorageKey("f95gm.settings.pageSize")
    val CARD_SIZE = StorageKey("f95gm.settings.cardSize")
}
