package f95gm.server.upstream.request

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.kernel.SingleFlight
import f95gm.server.state.runtime.session.SessionLockRegistry
import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import f95gm.server.config.UpstreamConcurrencyLimit

/** Isolates request coalescing and per-session serialization from transport policy. */
internal class UpstreamConcurrencyDispatcher(
    private val sessionLocks: SessionLockRegistry,
    private val getFlights: SingleFlight<UpstreamFlightKey, UpstreamResponse>,
    maxConcurrentRequests: Int = DEFAULT_MAX_CONCURRENT_REQUESTS
) {
    private val concurrencyLimit = UpstreamConcurrencyLimit(maxConcurrentRequests)
    private val permits = Semaphore(concurrencyLimit.value)

    init {
        require(maxConcurrentRequests > 0) { "Maximum upstream concurrency must be positive" }
    }

    suspend fun dispatch(
        method: HttpMethod,
        path: UpstreamPath,
        session: UpstreamSession,
        body: UpstreamBody?,
        contentType: UpstreamContentType?,
        headers: List<UpstreamHeader>,
        operation: suspend () -> UpstreamResponse
    ): UpstreamResponse {
        if (method != HttpMethod.Get) return permits.withPermit { sessionLocks.withLock(session.browserId) { operation() } }
        val key = listOf(
            session.browserId,
            method.value,
            path.text,
            body?.text.orEmpty(),
            contentType?.value.orEmpty(),
            headers.associate { it.name.text to it.value.text }.toSortedMap()
        ).joinToString("|")
        return permits.withPermit { getFlights.run(UpstreamFlightKey(key), operation) }
    }

    companion object {
        const val DEFAULT_MAX_CONCURRENT_REQUESTS = 32
    }
}
