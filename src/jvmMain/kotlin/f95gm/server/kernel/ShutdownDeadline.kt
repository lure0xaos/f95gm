package f95gm.server.kernel

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Propagates one shutdown budget through nested awaitable resources. */
internal class ShutdownDeadline(timeout: Duration) : AbstractCoroutineContextElement(Key) {
    private val deadline: TimeMark = TimeSource.Monotonic.markNow() + timeout

    suspend fun remainingOr(default: Duration): Duration =
        (-deadline.elapsedNow()).coerceIn(Duration.ZERO, default)

    companion object Key : CoroutineContext.Key<ShutdownDeadline>
}

internal suspend fun remainingShutdownTimeout(default: Duration): Duration =
    currentCoroutineContext()[ShutdownDeadline]?.remainingOr(default) ?: default
