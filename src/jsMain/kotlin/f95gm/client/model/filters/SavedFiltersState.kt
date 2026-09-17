package f95gm.client.model.filters

import f95gm.client.state.runtime.AsyncState
import f95gm.client.state.runtime.isLoading
import f95gm.client.state.runtime.errorMessage

internal data class SavedFiltersState(
    val items: List<SavedFilter> = emptyList(),
    val async: AsyncState<Unit> = AsyncState.Idle
)

internal val SavedFiltersState.loading: Boolean get() = async.isLoading
internal val SavedFiltersState.error: String get() = async.errorMessage
