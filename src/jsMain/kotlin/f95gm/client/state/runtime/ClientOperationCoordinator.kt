package f95gm.client.state.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** Owns operation scopes, request freshness, and the keyed job registry. */
internal class ClientOperationCoordinator(
    parentScope: CoroutineScope,
    private val statusSink: ClientOperationStatusSink
) {
    private val parentContext = parentScope.coroutineContext
    private var clientScope = newClientScope()
    private var jobs = ClientJobRegistry()
    private val requestTokens = mutableMapOf<ClientJobKey, Long>()
    private val operationGenerations = mutableMapOf<ClientJobKey, Long>()

    val scope: CoroutineScope get() = clientScope

    fun launch(
        key: ClientJobKey,
        block: suspend () -> Unit
    ): Job {
        val generation = (operationGenerations[key] ?: 0L) + 1L
        operationGenerations[key] = generation
        statusSink.publish(key, AsyncState.Loading)
        val job = clientScope.launch { block() }
        jobs.replace(key, job)
        job.invokeOnCompletion { cause ->
            if (currentJob(key) === job && operationGenerations[key] == generation) {
                val status = when {
                    cause == null -> AsyncState.Success(Unit)
                    cause is CancellationException -> AsyncState.Cancelled
                    else -> AsyncState.Failure("operation_failed")
                }
                statusSink.publish(key, status)
                clearFreshness(key, generation)
            }
        }
        return job
    }

    fun replaceJob(key: ClientJobKey, job: Job) = jobs.replace(key, job)
    fun currentJob(key: ClientJobKey): Job? = jobs.current(key)
    fun beginRequest(key: ClientJobKey): Long =
        (requestTokens[key] ?: 0L).inc().also { requestTokens[key] = it }
    fun isCurrentRequest(key: ClientJobKey, token: Long): Boolean = requestTokens[key] == token
    fun isJobActive(key: ClientJobKey): Boolean = jobs.isActive(key)

    fun cancelJob(key: ClientJobKey) {
        operationGenerations[key] = (operationGenerations[key] ?: 0L) + 1L
        clearFreshness(key)
        jobs.cancel(key)
    }

    fun cancelAll() {
        jobs.cancelAll()
        requestTokens.clear()
        operationGenerations.clear()
    }

    fun replaceScope() {
        closeOperations()
        jobs = ClientJobRegistry()
        requestTokens.clear()
        operationGenerations.clear()
        clientScope = newClientScope()
    }

    fun close() {
        closeOperations()
    }

    private fun closeOperations() {
        jobs.close()
        clientScope.cancel()
        requestTokens.clear()
        operationGenerations.clear()
    }

    private fun clearFreshness(key: ClientJobKey, generation: Long? = null) {
        if (generation == null || operationGenerations[key] == generation) {
            requestTokens.remove(key)
            operationGenerations.remove(key)
        }
    }

    private fun newClientScope(): CoroutineScope {
        val parentJob = checkNotNull(parentContext[Job])
        val context: CoroutineContext = parentContext.minusKey(Job) + SupervisorJob(parentJob)
        return CoroutineScope(context)
    }
}
