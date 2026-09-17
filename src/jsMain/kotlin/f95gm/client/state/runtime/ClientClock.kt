package f95gm.client.state.runtime

import kotlinx.coroutines.delay
import kotlinx.browser.window
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds

internal interface ClientClock {
    fun now(): Instant
    fun monotonic(): Duration
    suspend fun delay(value: Duration)
}

internal object SystemClientClock : ClientClock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(kotlin.js.Date.now().toLong())
    override fun monotonic(): Duration = window.performance.now().milliseconds
    override suspend fun delay(value: Duration) = kotlinx.coroutines.delay(value)
}
