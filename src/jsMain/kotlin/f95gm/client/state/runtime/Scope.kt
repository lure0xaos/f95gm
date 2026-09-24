package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlinx.coroutines.*

private fun newClientScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

internal class ClientController {
    private var clientScope = newClientScope()
    private var lifecycle = ClientLifecycleState()
    private var probeNumber = 0
    private var jobs = ClientJobRegistry()

    val scope: CoroutineScope get() = clientScope
    fun replaceJob(key: ClientJobKey, job: kotlinx.coroutines.Job) = jobs.replace(key, job)
    fun currentJob(key: ClientJobKey): kotlinx.coroutines.Job? = jobs.current(key)
    fun isJobActive(key: ClientJobKey): Boolean = jobs.isActive(key)
    fun cancelJob(key: ClientJobKey) {
        jobs.cancel(key)
    }

    fun cancelAllJobs() {
        jobs.cancelAll()
    }

    fun replaceScope() {
        clientScope.cancel()
        jobs.close()
        jobs = ClientJobRegistry()
        clientScope = newClientScope()
    }

    var sessionLoaded: Boolean
        get() = isLoaded(ClientLoadState.SESSION)
        set(value) = setLoaded(ClientLoadState.SESSION, value)
    fun markSessionLoaded() = setLoaded(ClientLoadState.SESSION, true)
    fun clearSession() = resetSessionLifecycle()
    var loadedDetailId: F95ThreadId?
        get() = lifecycle.detailId
        set(value) {
            lifecycle = lifecycle.copy(detailId = value)
        }

    var loadedOptionsCategory: F95Category?
        get() = lifecycle.optionsCategory
        set(value) {
            lifecycle = lifecycle.copy(optionsCategory = value)
        }

    var marksLoaded: Boolean
        get() = isLoaded(ClientLoadState.MARKS)
        set(value) = setLoaded(ClientLoadState.MARKS, value)
    fun markMarksLoaded() = setLoaded(ClientLoadState.MARKS, true)
    fun clearMarks() = setLoaded(ClientLoadState.MARKS, false)
    var savedFiltersLoaded: Boolean
        get() = isLoaded(ClientLoadState.SAVED_FILTERS)
        set(value) = setLoaded(ClientLoadState.SAVED_FILTERS, value)
    fun markSavedFiltersLoaded() = setLoaded(ClientLoadState.SAVED_FILTERS, true)
    fun clearSavedFilters() = setLoaded(ClientLoadState.SAVED_FILTERS, false)
    var appliedFilterId: F95FilterId?
        get() = lifecycle.appliedFilterId
        set(value) {
            lifecycle = lifecycle.copy(appliedFilterId = value)
        }

    var healthProbeNumber: Int
        get() = probeNumber
        set(value) {
            probeNumber = value
        }

    fun resetSessionLifecycle() {
        lifecycle = ClientLifecycleState()
    }

    private fun isLoaded(state: ClientLoadState): Boolean = state in lifecycle.loaded
    private fun setLoaded(state: ClientLoadState, value: Boolean) {
        lifecycle = lifecycle.copy(
            loaded = if (value) lifecycle.loaded + state else lifecycle.loaded - state
        )
    }
}

internal val clientController = ClientController()

internal val scope: CoroutineScope get() = clientController.scope

internal fun startClientScope() {
    if (!scope.isActive) clientController.replaceScope()
}

internal fun cancelClientScope() {
    clientController.cancelAllJobs()
    scope.cancel()
}

internal fun resetClientSessionLifecycle() {
    clientController.resetSessionLifecycle()
}
