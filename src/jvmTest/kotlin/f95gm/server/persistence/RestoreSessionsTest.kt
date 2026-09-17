package f95gm.server.persistence

import f95gm.domain.values.F95SessionId
import f95gm.server.application.SessionRegistry
import f95gm.server.config.ServerDefaults
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.database.migrateDatabase
import f95gm.server.persistence.sessions.restoreSessions
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.session.SessionLookup
import f95gm.server.state.runtime.MutableServerClock
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RestoreSessionsTest {
    @Test
    fun restoresValidSessionsAndQuarantinesExpiredRowsAndUnsafeCookies() = runTest {
        val database = Database.connect(testDatabaseUrl("restore_sessions"), driver = "org.h2.Driver")
        migrateDatabase(database)
        val now = 1_700_000_000_000L
        val clock = MutableServerClock(initialMillis = now, initialMonotonicNanos = 5_000_000L)
        val validId = "00000000-0000-0000-0000-000000000031"
        val expiredId = "00000000-0000-0000-0000-000000000032"
        transaction(database) {
            SessionsTable.insert { it[id] = validId; it[accountKey] = "account"; it[updatedAt] = now - 1_000L }
            SessionsTable.insert { it[id] = expiredId; it[accountKey] = "expired"; it[updatedAt] = now - (ServerDefaults.SESSION_MAX_AGE_SECONDS * 1000L + 1L) }
            SessionCookiesTable.insert { it[sessionId] = validId; it[name] = "xf_user"; it[value] = "safe" }
            SessionCookiesTable.insert { it[sessionId] = validId; it[name] = "bad"; it[value] = "unsafe;cookie" }
        }
        val registry = SessionRegistry(clock)
        val boundary = DatabaseExecutor(
            database = { database },
            executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext(),
            clock = clock
        )

        restoreSessions(boundary, registry, clock)

        val restored = assertIs<SessionLookup.Found>(registry.lookup(F95SessionId(validId), MonotonicMillis(5L))).session
        assertEquals("account", restored.accountKey.value)
        assertEquals("xf_user=safe", restored.header())
        assertIs<SessionLookup.Missing>(registry.lookup(F95SessionId(expiredId), MonotonicMillis(5L)))
        assertTrue(transaction(database) { SessionsTable.selectAll().map { it[SessionsTable.id] }.toList() == listOf(validId) })
    }
}
