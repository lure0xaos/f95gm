package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlinx.coroutines.Job

/** Request tracking never exposes controller shutdown or scope replacement. */
internal interface ClientRequestTrackingPort {
    fun beginRequest(key: ClientJobKey): Long
    fun isCurrentRequest(key: ClientJobKey, token: Long): Boolean
    fun currentJob(key: ClientJobKey): Job?
    fun isJobActive(key: ClientJobKey): Boolean
    fun cancelJob(key: ClientJobKey)
}

/** A loading flag is bound to one feature at composition time. */
internal interface ClientFeatureLoadingPort {
    fun isLoaded(): Boolean
    fun markLoaded()
    fun clearLoaded()
}

internal interface ClientCatalogLoadingPort : ClientFeatureLoadingPort {
    var loadedOptionsCategory: F95Category?
}

internal interface ClientFiltersLoadingPort : ClientFeatureLoadingPort {
    var appliedFilterId: F95FilterId?
}

internal interface ClientDetailLoadingPort {
    var loadedDetailId: F95ThreadId?
}

internal fun interface ClientSessionLifecyclePort {
    fun resetSessionLifecycle()
}

internal fun interface ClientHealthProbePort {
    fun nextProbeNumber(): Int
}
