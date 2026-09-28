package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.upstream.request.UpstreamPolicy
import kotlin.time.Duration.Companion.milliseconds

internal data class ServerHttpPolicies(
    val clientTimeouts: HttpClientTimeoutPolicy
) {
    companion object {
        fun from(config: ServerConfig): ServerHttpPolicies = ServerHttpPolicies(
            clientTimeouts = HttpClientTimeoutPolicy(
                requestMillis = config.timeouts.requestMillis,
                connectMillis = config.timeouts.connectMillis,
                socketMillis = config.timeouts.socketMillis
            )
        )
    }
}

/** Upstream request retry policy is separate from local HTTP client transport settings. */
internal data class ServerUpstreamPolicies(val request: UpstreamPolicy) {
    companion object {
        fun from(config: ServerConfig): ServerUpstreamPolicies = ServerUpstreamPolicies(
            UpstreamPolicy(
                maxAttempts = config.upstreamRetry.maxAttempts,
                maxResponseBytes = config.limits.maxMediaResponseBytes,
                retryBaseDelay = config.upstreamRetry.baseDelayMillis.value.milliseconds,
                retryMaxDelay = config.upstreamRetry.maxDelayMillis.value.milliseconds,
                maxTotalRetryDelay = config.upstreamRetry.maxTotalDelayMillis.value.milliseconds,
                requestTimeout = config.timeouts.requestMillis.value.milliseconds
            )
        )
    }
}
