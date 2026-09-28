package f95gm.server.upstream.request

import f95gm.server.config.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpstreamEndpointPolicyTest {
    @Test
    fun defaultEndpointUsesClientPolicyWhileNamedEndpointsAreExplicit() {
        assertNull(UpstreamEndpointPolicy.DEFAULT.value)
        val configured = UpstreamPolicy(maxAttempts = 4, maxResponseBytes = Bytes(16L * 1024L * 1024L))
        UpstreamEndpointPolicy.entries.forEach { endpoint ->
            check(endpoint.resolve(configured).maxResponseBytes == configured.maxResponseBytes)
        }
    }

    @Test
    fun endpointPoliciesKeepTheirSafetyInvariants() {
        val configured = UpstreamPolicy(maxAttempts = 4, requestTimeout = kotlin.time.Duration.Companion.seconds(45))
        assertEquals(1, UpstreamEndpointPolicy.LOGIN.resolve(configured).maxAttempts)
        assertEquals(2, UpstreamEndpointPolicy.MEDIA.resolve(configured).maxAttempts)
        assertEquals(45L, UpstreamEndpointPolicy.DETAIL.resolve(configured).requestTimeout.inWholeSeconds)
    }
}
