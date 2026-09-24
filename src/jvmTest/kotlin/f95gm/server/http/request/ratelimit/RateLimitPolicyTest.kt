package f95gm.server.http.request.ratelimit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RateLimitPolicyTest {
    @Test
    fun validatesIndependentBudgets() {
        assertFailsWith<IllegalArgumentException> { RateLimitPolicy.configured(0, 1, 1_000) }
        assertFailsWith<IllegalArgumentException> { RateLimitPolicy.configured(1, 0, 1_000) }
        assertFailsWith<IllegalArgumentException> { RateLimitPolicy.configured(1, 1, 0) }
    }

    @Test
    fun keepsConfiguredIpAndAccountBudgetsSeparate() {
        val policy = RateLimitPolicy.configured(120, 7, 60_000)
        assertEquals(120, policy.ipRequestsPerWindow.value)
        assertEquals(7, policy.accountRequestsPerWindow.value)
        assertEquals(60_000, policy.windowMillis.value)
    }
}
