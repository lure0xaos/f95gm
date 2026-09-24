package f95gm.server.persistence.database

import java.sql.SQLTransientException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration

class DatabaseRetryTest {
    @Test
    fun transientConnectionFailuresRespectRetryLimit() {
        var calls = 0
        val executor = DatabaseExecutor(
            database = { calls++; throw SQLTransientException("temporary") },
            retryPolicy = DatabaseRetryPolicy(maxRetries = 2, retryDelay = Duration.ZERO)
        )
        assertFailsWith<SQLTransientException> { executor.transaction { 1 } }
        assertEquals(3, calls)
    }

    @Test
    fun nonTransientFailuresAreNotRetried() {
        var calls = 0
        val executor = DatabaseExecutor(
            database = { calls++; error("permanent") },
            retryPolicy = DatabaseRetryPolicy(maxRetries = 2, retryDelay = Duration.ZERO)
        )
        assertFailsWith<IllegalStateException> { executor.transaction { 1 } }
        assertEquals(1, calls)
    }
}
