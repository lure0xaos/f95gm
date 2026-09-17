package f95gm.server.http.request.ratelimit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestRateLimitTest {
    @Test
    fun acceptsPositiveBudgetsAndRejectsZeroOrNegativeValues() {
        assertEquals(5, RequestRateLimit(5).value)
        assertFailsWith<IllegalArgumentException> { RequestRateLimit(0) }
        assertFailsWith<IllegalArgumentException> { RequestRateLimit(-1) }
    }
}
