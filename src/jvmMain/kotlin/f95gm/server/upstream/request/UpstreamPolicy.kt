package f95gm.server.upstream.request

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal data class UpstreamPolicy(
    val maxAttempts: Int = 3,
    val maxResponseBytes: Long = 4L * 1024L * 1024L,
    val retryBaseDelay: Duration = 250.milliseconds,
    val retryMaxDelay: Duration = 30.seconds,
    val requestTimeout: Duration = 30.seconds
) {
    init {
        require(maxAttempts > 0)
        require(maxResponseBytes > 0)
        require(retryBaseDelay.isPositive())
        require(retryMaxDelay >= retryBaseDelay)
        require(requestTimeout.isPositive())
    }
}
