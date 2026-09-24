package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

internal data class ClientLifecycleState(
    val loaded: Set<ClientLoadState> = emptySet(),
    val detailId: F95ThreadId? = null,
    val optionsCategory: F95Category? = null,
    val appliedFilterId: F95FilterId? = null
)
