package f95gm.client.model.catalog

import f95gm.client.state.runtime.AsyncState
import f95gm.client.state.runtime.isLoading
import f95gm.client.state.runtime.errorMessage

internal data class CatalogOptions(
    val tags: List<TagOption> = emptyList(),
    val prefixes: List<PrefixOption> = emptyList(),
    val prefixGroups: List<FilterGroup> = emptyList(),
    val async: AsyncState<Unit> = AsyncState.Idle
)

internal val CatalogOptions.loading: Boolean get() = async.isLoading
internal val CatalogOptions.error: String get() = async.errorMessage
internal val CatalogOptions.asyncState: AsyncState<Unit> get() = async
