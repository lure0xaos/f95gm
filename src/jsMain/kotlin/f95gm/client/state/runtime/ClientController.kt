package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

internal class ClientController : AutoCloseable, ClientRequestTrackingPort {
    private val storeJob = SupervisorJob()
    val rootStoreScope: CoroutineScope = CoroutineScope(storeJob + Dispatchers.Main.immediate)
    private var lifecycle = ClientLifecycleState()
    private var probeNumber = 0
    private val operations = ClientOperationCoordinator(
        rootStoreScope,
        ClientOperationStatusSink(::publishOperationStatus)
    )
    private var operationStatusSink: ClientOperationStatusSink? = null

    val scope: CoroutineScope get() = operations.scope
    val rootStoreJob: Job get() = storeJob
    fun setOperationStatusSink(sink: ClientOperationStatusSink) {
        operationStatusSink = sink
    }
    private fun publishOperationStatus(key: ClientJobKey, status: ClientOperationStatus) {
        lifecycle = lifecycle.copy(operationStatuses = lifecycle.operationStatuses + (key to status))
        operationStatusSink?.publish(key, status)
    }
    fun launchOperation(key: ClientJobKey, block: suspend () -> Unit): Job =
        operations.launch(key, block)
    fun launchOperationHandle(key: ClientJobKey, block: suspend () -> Unit): ClientJobHandle {
        launchOperation(key, block)
        return ClientJobHandle(key)
    }
    fun replaceJob(key: ClientJobKey, job: Job) = operations.replaceJob(key, job)
    override fun currentJob(key: ClientJobKey): Job? = operations.currentJob(key)
    override fun beginRequest(key: ClientJobKey): Long = operations.beginRequest(key)
    override fun isCurrentRequest(key: ClientJobKey, token: Long): Boolean = operations.isCurrentRequest(key, token)
    override fun isJobActive(key: ClientJobKey): Boolean = operations.isJobActive(key)
    fun loadStatus(state: ClientLoadState): ClientLoadStatus = lifecycle.loadLifecycle.status(state)
    fun operationStatus(key: ClientJobKey): ClientOperationStatus =
        lifecycle.operationStatuses[key] ?: AsyncState.Idle
    override fun cancelJob(key: ClientJobKey) {
        operations.cancelJob(key)
        publishOperationStatus(key, AsyncState.Cancelled)
    }

    fun cancelAllJobs() {
        operations.cancelAll()
        lifecycle.operationStatuses
            .filterValues { it == AsyncState.Loading }
            .keys
            .forEach { key -> publishOperationStatus(key, AsyncState.Cancelled) }
    }

    fun replaceScope() {
        check(storeJob.isActive) { "Client root stores are closed; create a new controller" }
        cancelOperations()
        operations.replaceScope()
    }

    override fun close() {
        cancelOperations()
        storeJob.cancel()
    }

    private fun cancelOperations() {
        operations.close()
        lifecycle.operationStatuses
            .filterValues { it == AsyncState.Loading }
            .keys
            .forEach { key -> publishOperationStatus(key, AsyncState.Cancelled) }
    }

    fun isLoaded(state: ClientLoadState): Boolean = loadStatus(state) == ClientLoadStatus.LOADED
    fun markLoaded(state: ClientLoadState) { lifecycle = lifecycle.copy(loadLifecycle = lifecycle.loadLifecycle.markLoaded(state)) }
    fun clearLoaded(state: ClientLoadState) { lifecycle = lifecycle.copy(loadLifecycle = lifecycle.loadLifecycle.clear(state)) }
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

    fun clearMarks() = clearLoaded(ClientLoadState.MARKS)
    fun clearSavedFilters() = clearLoaded(ClientLoadState.SAVED_FILTERS)
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

}
