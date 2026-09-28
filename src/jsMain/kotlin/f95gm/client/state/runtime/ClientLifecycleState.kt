package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

internal data class ClientLifecycleState(
    val loadLifecycle: ClientLoadLifecycle = ClientLoadLifecycle(),
    val operationStatuses: Map<ClientJobKey, ClientOperationStatus> = emptyMap(),
    val detailId: F95ThreadId? = null,
    val optionsCategory: F95Category? = null,
    val appliedFilterId: F95FilterId? = null
)
