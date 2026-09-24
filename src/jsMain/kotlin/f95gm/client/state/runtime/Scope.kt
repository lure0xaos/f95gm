package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import kotlinx.coroutines.*

private fun newClientScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

internal class ClientController {
    private var clientScope = newClientScope()
    private var lifecycle = ClientLifecycleState()
    private var healthProbeNumber = 0
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

    fun isSessionLoaded(): Boolean = isLoaded(ClientLoadState.SESSION)
    fun setSessionLoaded(value: Boolean) = setLoaded(ClientLoadState.SESSION, value)
    fun markSessionLoaded() = setLoaded(ClientLoadState.SESSION, true)
    fun clearSession() = resetSessionLifecycle()
    fun loadedDetailId(): f95gm.domain.values.F95ThreadId? = lifecycle.detailId
    fun setLoadedDetailId(value: f95gm.domain.values.F95ThreadId?) {
        lifecycle = lifecycle.copy(detailId = value)
    }

    fun loadedOptionsCategory(): F95Category? = lifecycle.optionsCategory
    fun setLoadedOptionsCategory(value: F95Category?) {
        lifecycle = lifecycle.copy(optionsCategory = value)
    }

    fun areMarksLoaded(): Boolean = isLoaded(ClientLoadState.MARKS)
    fun setMarksLoaded(value: Boolean) = setLoaded(ClientLoadState.MARKS, value)
    fun markMarksLoaded() = setLoaded(ClientLoadState.MARKS, true)
    fun clearMarks() = setLoaded(ClientLoadState.MARKS, false)
    fun areSavedFiltersLoaded(): Boolean = isLoaded(ClientLoadState.SAVED_FILTERS)
    fun setSavedFiltersLoaded(value: Boolean) = setLoaded(ClientLoadState.SAVED_FILTERS, value)
    fun markSavedFiltersLoaded() = setLoaded(ClientLoadState.SAVED_FILTERS, true)
    fun clearSavedFilters() = setLoaded(ClientLoadState.SAVED_FILTERS, false)
    fun appliedFilterId(): f95gm.domain.values.F95FilterId? = lifecycle.appliedFilterId
    fun setAppliedFilterId(value: f95gm.domain.values.F95FilterId?) {
        lifecycle = lifecycle.copy(appliedFilterId = value)
    }

    fun healthProbeNumber(): Int = healthProbeNumber
    fun setHealthProbeNumber(value: Int) {
        healthProbeNumber = value
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
