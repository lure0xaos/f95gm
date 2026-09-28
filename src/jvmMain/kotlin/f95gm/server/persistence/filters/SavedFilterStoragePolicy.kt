package f95gm.server.persistence.filters

import f95gm.domain.values.F95AccountKey
import f95gm.server.config.CatalogLimits
import f95gm.server.config.constants.FilterLimits
import f95gm.server.config.constants.ServerStorageLimits
import f95gm.server.model.filters.SavedFilter
import f95gm.server.persistence.model.requireStorageLength

/** Keeps database-length and truncation policy out of transaction orchestration. */
internal object SavedFilterStoragePolicy {
    fun validate(account: F95AccountKey, filter: SavedFilter) {
        requireStorageLength(account.value, ServerStorageLimits.ACCOUNT_KEY_LENGTH, "accountKey")
        requireStorageLength(filter.name.value, FilterLimits.FILTER_NAME_LENGTH, "filterName")
    }

    fun accountKey(account: F95AccountKey): String = account.value.take(ServerStorageLimits.ACCOUNT_KEY_LENGTH)
    fun name(name: String): String = name.take(FilterLimits.FILTER_NAME_LENGTH)
    fun search(value: String): String = value.take(CatalogLimits.MAX_TEXT_LENGTH)
}
