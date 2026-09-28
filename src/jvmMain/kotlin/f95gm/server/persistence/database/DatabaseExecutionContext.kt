package f95gm.server.persistence.database

import f95gm.server.kernel.ServerClock
import f95gm.server.platform.SystemServerClock
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.locks.LockSupport
import kotlin.time.Duration

internal fun interface BlockingDelayScheduler {
    fun await(delay: Duration)
}

internal interface DatabaseExecutionContext {
    val dispatcher: CoroutineDispatcher
}

internal class InterruptibleBlockingDelayScheduler(
    private val clock: ServerClock = SystemServerClock
) : BlockingDelayScheduler {
    override fun await(delay: Duration) {
        var remaining = delay.inWholeNanoseconds.coerceAtLeast(0L)
        val started = clock.monotonicTimestamp()
        while (remaining > 0L) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Blocking retry delay interrupted")
            LockSupport.parkNanos(remaining)
            remaining = (delay - clock.elapsedSince(started)).inWholeNanoseconds.coerceAtLeast(0L)
        }
    }
}
