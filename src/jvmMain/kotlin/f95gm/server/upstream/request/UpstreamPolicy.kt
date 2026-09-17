package f95gm.server.upstream.request

import f95gm.server.config.Bytes
import f95gm.server.kernel.RetryAttemptLimit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal sealed interface UpstreamPolicySelection {
    data object UseClientDefault : UpstreamPolicySelection {
        override val value: UpstreamPolicy? get() = null
    }
    val value: UpstreamPolicy?
    fun resolve(default: UpstreamPolicy): UpstreamPolicy = value ?: default
}

internal data class UpstreamPolicy(
    val maxAttempts: Int = 3,
    val maxResponseBytes: Bytes = Bytes(4L * 1024L * 1024L),
    val retryBaseDelay: Duration = 250.milliseconds,
    val retryMaxDelay: Duration = 30.seconds,
    val maxTotalRetryDelay: Duration = 60.seconds,
    val requestTimeout: Duration = 30.seconds
) : UpstreamPolicySelection {
    val attemptLimit: RetryAttemptLimit get() = RetryAttemptLimit(maxAttempts)
    val deadline: RequestDeadline get() = RequestDeadline(requestTimeout)
    override val value: UpstreamPolicy get() = this
    init {
        require(maxAttempts > 0)
        require(maxResponseBytes.value > 0)
        require(retryBaseDelay.isPositive())
        require(retryMaxDelay >= retryBaseDelay)
        require(!maxTotalRetryDelay.isNegative())
        require(requestTimeout.isPositive())
    }
}

@JvmInline
internal value class RequestDeadline(val duration: Duration) {
    init { require(duration.isPositive()) { "Request deadline must be positive" } }
}

/** Shared response safety policy; endpoint policies may only specialize its bounded limit. */
internal data class UpstreamResponsePolicy(val maxBodyBytes: Bytes) {
    init { require(maxBodyBytes.value > 0) }
}

internal enum class UpstreamEndpointPolicy(override val value: UpstreamPolicy?) : UpstreamPolicySelection {
    DEFAULT(null),
    LOGIN(UpstreamPolicy(maxAttempts = 1, requestTimeout = 20.seconds)),
    CATALOG(null),
    DETAIL(null),
    MEDIA(UpstreamPolicy(maxAttempts = 2));

    override fun resolve(default: UpstreamPolicy): UpstreamPolicy = when (this) {
        DEFAULT, CATALOG, DETAIL -> default
        LOGIN -> default.copy(maxAttempts = 1, requestTimeout = 20.seconds)
        MEDIA -> default.copy(maxAttempts = 2)
    }
}
