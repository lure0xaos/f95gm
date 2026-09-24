package f95gm.server.state.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

internal class SingleFlight<K, V>(
    private val scope: CoroutineScope
) : LifecycleResource {
    private val flights = ConcurrentHashMap<K, CompletableDeferred<V>>()
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed

    suspend fun run(key: K, block: suspend () -> V): V {
        check(!closeGuard.isClosed) { "Single-flight registry is closed" }
        val created = CompletableDeferred<V>()
        val flight = flights.putIfAbsent(key, created) ?: created
        if (flight !== created) return flight.await()
        if (closeGuard.isClosed) {
            flights.remove(key, created)
            created.completeExceptionally(CancellationException("Single-flight registry is closed"))
            return created.await()
        }
        scope.launch {
            try {
                created.complete(block())
            } catch (error: CancellationException) {
                created.completeExceptionally(error)
                throw error
            } catch (error: Exception) {
                created.completeExceptionally(error)
                throw error
            } catch (error: Error) {
                created.completeExceptionally(error)
                throw error
            } finally {
                flights.remove(key, created)
            }
        }
        return created.await()
    }

    override fun close() {
        closeGuard.closeOnce {
            flights.values.forEach { it.completeExceptionally(CancellationException("Single-flight registry is closed")) }
            flights.clear()
        }
    }
}
