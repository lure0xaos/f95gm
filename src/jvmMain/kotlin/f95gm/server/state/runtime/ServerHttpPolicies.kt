package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.upstream.request.UpstreamPolicy
import kotlin.time.Duration.Companion.milliseconds

internal data class ServerHttpPolicies(
    val clientTimeouts: HttpClientTimeoutPolicy,
    val upstream: UpstreamPolicy
) {
    companion object {
        fun from(config: ServerConfig): ServerHttpPolicies = ServerHttpPolicies(
            clientTimeouts = HttpClientTimeoutPolicy(
                requestMillis = config.timeouts.requestMillis,
                connectMillis = config.timeouts.connectMillis,
                socketMillis = config.timeouts.socketMillis
            ),
            upstream = UpstreamPolicy(
                maxAttempts = config.retry.maxAttempts,
                retryBaseDelay = config.retry.baseDelayMillis.value.milliseconds,
                retryMaxDelay = config.retry.maxDelayMillis.value.milliseconds,
                requestTimeout = config.timeouts.requestMillis.value.milliseconds
            )
        )
    }
}
