package f95gm.domain.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ApiParsingPoliciesTest {
    @Test
    fun integerParsersDistinguishPositiveNonNegativeAndWhitespaceRules() {
        assertEquals(0, "0".parseIntOrNull())
        assertEquals(-2, "-2".parseIntOrNull())
        assertNull(" 4 ".parseIntOrNull())
        assertEquals(0, "0".parseNonNegativeInt())
        assertNull("-1".parseNonNegativeInt())
        assertEquals(5, "5".parsePositiveInt())
        assertNull("0".parsePositiveInt())
        assertEquals(9L, " 9 ".parsePositiveLong())
        assertNull("0".parsePositiveLong())
        assertEquals(0L, " 0 ".parseNonNegativeLong())
        assertNull("-1".parseNonNegativeLong())
        assertNull("999999999999999999999999".parsePositiveLong())
    }

    @Test
    fun retryAfterParsesDeltaSecondsAndHttpDatesAgainstInjectedClock() {
        assertEquals(RetryAfterHint(12, RetryAfterSource.DELTA_SECONDS), parseRetryAfterSeconds(" 12 "))
        assertEquals(RetryAfterHint(0, RetryAfterSource.DELTA_SECONDS), parseRetryAfterSeconds("0"))
        assertNull(parseRetryAfterSeconds("-1"))
        assertNull(parseRetryAfterSeconds("invalid"))
        val now = 1_700_000_000_000L
        assertEquals(RetryAfterHint(10, RetryAfterSource.HTTP_DATE), parseRetryAfter("Tue, 14 Nov 2023 22:13:30 GMT", now))
        assertNull(parseRetryAfter("invalid", now))
        assertNull(parseRetryAfter(null, now))
    }

    @Test
    fun retryBudgetAndDelayPolicyClampAndRejectInvalidConfiguration() {
        val budget = RetryBudget(1.seconds)
        assertEquals(700.milliseconds, budget.reserve(700.milliseconds))
        assertEquals(300.milliseconds, budget.remaining)
        assertNull(budget.reserve(301.milliseconds))
        assertEquals(300.milliseconds, budget.remaining)
        assertEquals(0.seconds, budget.reserve((-1).seconds))

        val policy = RetryDelayPolicy(100.milliseconds, 500.milliseconds, RetryJitter { _, _ -> 50.milliseconds })
        assertEquals(150.milliseconds, policy.delay(0))
        assertEquals(500.milliseconds, policy.delay(10))
        assertEquals(0.milliseconds, policy.delay(1, (-1).seconds))
        assertEquals(200.milliseconds, policy.delay(1, 200.milliseconds))
        assertFailsWith<IllegalArgumentException> { RetryBudget((-1).seconds) }
        assertFailsWith<IllegalArgumentException> { RetryDelayPolicy(2.seconds, 1.seconds) }
        assertFailsWith<IllegalArgumentException> { policy.delay(-1) }
    }
}
