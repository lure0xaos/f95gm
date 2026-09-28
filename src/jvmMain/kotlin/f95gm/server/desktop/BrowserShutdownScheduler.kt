package f95gm.server.desktop

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal class BrowserShutdownScheduler {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "f95gm-browser-shutdown").apply { isDaemon = true }
    }

    fun schedule(delay: Duration, action: () -> Unit): ScheduledFuture<*> =
        executor.schedule({ action() }, delay.inWholeNanoseconds, TimeUnit.NANOSECONDS)

    fun cancel(task: ScheduledFuture<*>?) {
        task?.cancel(false)
    }

    fun close() {
        executor.shutdownNow()
    }
}
