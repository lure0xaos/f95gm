package f95gm.server.http.request.ratelimit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestBudgetTest {
    @Test
    fun acceptsPositiveBudgetsAndRejectsZeroOrNegativeValues() {
        assertEquals(5, RequestBudget(5).value)
        assertFailsWith<IllegalArgumentException> { RequestBudget(0) }
        assertFailsWith<IllegalArgumentException> { RequestBudget(-1) }
    }
}
