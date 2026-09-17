package f95gm.server.observability

import kotlin.test.Test
import kotlin.test.assertTrue

class StructuredLogSchemaContractTest {
    @Test
    fun featureSchemasAreRegisteredInTheAuthoritativeSchema() {
        val registered = StructuredLogSchema.names
        assertTrue(AuditLogSchema.eventNames.all(registered::contains))
        assertTrue(LifecycleLogSchema.eventNames.all(registered::contains))
        assertTrue(UpstreamLogSchema.eventNames.all(registered::contains))
    }

    @Test
    fun everyAuditEventHasAnExplicitAllowlist() {
        AuditLogSchema.eventNames.forEach { event ->
            assertTrue(allowedStructuredFields(event).isNotEmpty(), "Missing fields for $event")
        }
    }

    @Test
    fun lifecycleEventsHaveAnExplicitAllowlist() {
        LifecycleLogSchema.eventNames.forEach { event ->
            assertTrue(allowedStructuredFields(event).isNotEmpty(), "Missing fields for $event")
        }
        assertTrue(allowedStructuredFields("lifecycle_metrics").contains("rollbackFailures"))
    }
}
