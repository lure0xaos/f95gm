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
        val policies = ServerHttpPolicies.from(config)

        assertEquals(Millis(12_000), policies.clientTimeouts.requestMillis)
        assertEquals(Millis(2_000), policies.clientTimeouts.connectMillis)
        assertEquals(Millis(8_000), policies.clientTimeouts.socketMillis)
        assertEquals(4, policies.upstream.maxAttempts)
        assertEquals(100.milliseconds, policies.upstream.retryBaseDelay)
        assertEquals(900.milliseconds, policies.upstream.retryMaxDelay)
        assertEquals(12_000.milliseconds, policies.upstream.requestTimeout)
    }

    @Test
    fun derivesDefaultPoliciesFromDefaultConfiguration() {
        val policies = ServerHttpPolicies.from(ServerConfig.from(emptyMap()))

        assertEquals(Millis(30_000), policies.clientTimeouts.requestMillis)
        assertEquals(Millis(10_000), policies.clientTimeouts.connectMillis)
        assertEquals(Millis(30_000), policies.clientTimeouts.socketMillis)
        assertEquals(3, policies.upstream.maxAttempts)
        assertEquals(250.milliseconds, policies.upstream.retryBaseDelay)
        assertEquals(30_000.milliseconds, policies.upstream.retryMaxDelay)
        assertEquals(30_000.milliseconds, policies.upstream.requestTimeout)
    }
}
