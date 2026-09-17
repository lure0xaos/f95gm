package f95gm.server.state.runtime

import f95gm.server.config.Millis
import f95gm.server.config.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class ServerHttpPoliciesTest {
    @Test
    fun derivesClientAndUpstreamPoliciesFromOneConfiguration() {
        val config = ServerConfig.from(
            mapOf(
                "f95gm.requestTimeoutMillis" to "12_000".replace("_", ""),
                "f95gm.connectTimeoutMillis" to "2000",
                "f95gm.socketTimeoutMillis" to "8000",
                "f95gm.retryMaxAttempts" to "4",
                "f95gm.retryBaseDelayMillis" to "100",
                "f95gm.retryMaxDelayMillis" to "900"
            )
        )
        val httpPolicies = ServerHttpPolicies.from(config)
        val upstreamPolicies = ServerUpstreamPolicies.from(config)

        assertEquals(Millis(12_000), httpPolicies.clientTimeouts.requestMillis)
        assertEquals(Millis(2_000), httpPolicies.clientTimeouts.connectMillis)
        assertEquals(Millis(8_000), httpPolicies.clientTimeouts.socketMillis)
        assertEquals(4, upstreamPolicies.request.maxAttempts)
        assertEquals(100.milliseconds, upstreamPolicies.request.retryBaseDelay)
        assertEquals(900.milliseconds, upstreamPolicies.request.retryMaxDelay)
        assertEquals(12_000.milliseconds, upstreamPolicies.request.requestTimeout)
    }

    @Test
    fun derivesDefaultPoliciesFromDefaultConfiguration() {
        val httpPolicies = ServerHttpPolicies.from(ServerConfig.from(emptyMap()))
        val upstreamPolicies = ServerUpstreamPolicies.from(ServerConfig.from(emptyMap()))

        assertEquals(Millis(30_000), httpPolicies.clientTimeouts.requestMillis)
        assertEquals(Millis(10_000), httpPolicies.clientTimeouts.connectMillis)
        assertEquals(Millis(30_000), httpPolicies.clientTimeouts.socketMillis)
        assertEquals(3, upstreamPolicies.request.maxAttempts)
        assertEquals(250.milliseconds, upstreamPolicies.request.retryBaseDelay)
        assertEquals(30_000.milliseconds, upstreamPolicies.request.retryMaxDelay)
        assertEquals(30_000.milliseconds, upstreamPolicies.request.requestTimeout)
    }
}
