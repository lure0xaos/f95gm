package f95gm.server.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class BoundaryValueTypesTest {
    @Test
    fun rejectsInvalidBoundaryValues() {
        assertFailsWith<IllegalArgumentException> { DatabasePath("") }
        assertFailsWith<IllegalArgumentException> { RequestDuration((-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> { RetryAttempt(-1) }
        assertFailsWith<IllegalArgumentException> { RetryDelay((-1).milliseconds) }
    }

    @Test
    fun preservesTypedDurationValues() {
        assertEquals(250L, RequestDuration(250.milliseconds).millis)
    }
}
