package f95gm.server.observability

import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.LifecycleState
import f95gm.server.kernel.RetryFailureReason
import f95gm.server.model.http.ResponseBodyBytes
import f95gm.server.model.http.UpstreamFailure
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StructuredLogEventsTest {
    @Test
    fun serverLifecycleAndConfigurationEventsExposeTheirContractFields() {
        val started = ServerStartedEvent("http://127.0.0.1:8080/", "127.0.0.1", 8080, 12)
        assertEquals("server_started", started.name.value)
        assertTrue(started.fields.any { it.name == StructuredLogKeys.STARTUP_MILLIS && it.value?.text == "12" })
        assertTrue(structuredLog(started).contains("address=http://127.0.0.1:8080/"))

        val stopped = ServerStoppedEvent(120, 2, 10, 1, 3, 4, 0)
        assertEquals(7, stopped.fields.size)
        assertTrue(stopped.fields.any { it.name == StructuredLogKeys.ACTIVE_REQUESTS && it.value?.text == "2" })

        val lifecycle = LifecycleMetricsEvent(1, 2, 3)
        assertEquals(listOf("1", "2", "3"), lifecycle.fields.map { it.value?.text })

        val configuration = ConfigurationLoadedEvent("localhost", 8080, "db", true, "desktop", false)
        assertEquals(6, configuration.fields.size)
        assertTrue(configuration.fields.any { it.name == StructuredLogKeys.STATIC_DIRECTORY_CONFIGURED && it.value?.text == "true" })
    }

    @Test
    fun cacheEventsRepresentCountsAndFailureCategories() {
        assertEquals("1", CacheEvictionEvent().fields.single().value?.text)
        assertEquals("9", CacheEvictionEvent(9).fields.single().value?.text)
        val failure = CacheLoadFailureEvent("catalog", "timeout")
        assertEquals(listOf("catalog", "timeout"), failure.fields.map { it.value?.text })
        assertEquals("5", SessionEvictionEvent(5).fields.single().value?.text)
    }

    @Test
    fun upstreamDatabaseAndLifecycleEventsKeepTypedMeasurementsInTheirWireFields() {
        val retry = UpstreamRetryEvent("GET", "/media", RetryAttempt(2))
        assertEquals(listOf("GET", "/media", "2"), retry.fields.map { it.value?.text })
        val exhausted = UpstreamRetryBudgetExhaustedEvent(
            "GET", "/media", RetryFailureReason.DELAY_BUDGET_EXHAUSTED, 50.milliseconds, 10.milliseconds
        )
        assertEquals(listOf("GET", "/media", "delay_budget_exhausted", "50", "10"), exhausted.fields.map { it.value?.text })
        assertEquals("/media", UpstreamCircuitOpenEvent("/media").fields.single().value?.text)
        val failed = UpstreamTransportFailureEvent("GET", "/media", RetryAttempt(3), UpstreamFailure.TIMEOUT)
        assertEquals("timeout", failed.fields.last().value?.text)
        val response = UpstreamResponseEvent("GET", "/media", 200, "12", ResponseBodyBytes(12), RetryAttempt(1), RequestDurationMillis(9))
        assertEquals(listOf("GET", "/media", "200", "12", "12", "1", "9"), response.fields.map { it.value?.text })

        val database = DatabaseOperationEvent(DatabaseOperation.MARKS_STORE, 5, 3, DatabaseOperationOutcome.FAILURE, "SQLException")
        assertEquals("marks.store", database.fields[0].value?.text)
        assertEquals("2", database.fields[2].value?.text)
        assertEquals("failure", database.fields[3].value?.text)
        assertEquals("SQLException", database.fields[4].value?.text)
        assertEquals(DatabaseOperation.entries.map { it.wireName }.toSet(), DatabaseOperation.wireNames)

        val lifecycle = LifecycleStateChangedEvent(LifecycleResourceName("runtime"), LifecycleState.FAILED)
        assertEquals(listOf("runtime", "failed"), lifecycle.fields.map { it.value?.text })
        assertEquals("database_backup_created", DatabaseLifecycleEvent(DatabaseLifecycleEventName.BACKUP_CREATED).name.value)
        assertEquals("session_persisted", SessionLifecycleEvent(SessionLifecycleEventName.PERSISTED).name.value)
        assertEquals("audit_login_failed", AuditEvent(AuditEventName.LOGIN_FAILED, emptyList()).name.value)
    }
}
