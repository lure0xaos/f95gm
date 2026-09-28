package f95gm.server.observability

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MutationAuditTest {
    @Test
    fun markMutationContainsTheStableAuditFields() {
        val event = AuditEvent(
            AuditEventName.MARK_MUTATION,
            listOf(logField(StructuredLogKeys.REQUEST_ID, "req-1"), logField(StructuredLogKeys.ACCOUNT, "account-1"),
                logField(StructuredLogKeys.OPERATION, "store"), logField(StructuredLogKeys.OUTCOME, "success"))
        )

        val rendered = structuredLog(event)
        assertEquals("event=audit_mark_mutation", rendered.substringBefore(' '))
        assertContains(rendered, "requestId=req-1")
        assertContains(rendered, "account=account-1")
        assertContains(rendered, "operation=store")
        assertContains(rendered, "outcome=success")
    }

    @Test
    fun filterMutationUsesTheDedicatedEventName() {
        assertEquals(
            "event=audit_filter_mutation account=account operation=import outcome=success",
            structuredLog(
                AuditEvent(
                    AuditEventName.FILTER_MUTATION,
                    listOf(logField(StructuredLogKeys.ACCOUNT, "account"), logField(StructuredLogKeys.OPERATION, "import"), logField(StructuredLogKeys.OUTCOME, "success"))
                )
            )
        )
    }
}
