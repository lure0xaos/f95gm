package f95gm.server.routes.session

import f95gm.server.observability.AuditEvent
import f95gm.server.observability.AuditEventName
import f95gm.server.observability.AuditOutcome
import f95gm.server.observability.auditLog
import f95gm.server.observability.logField
import f95gm.server.observability.StructuredLogKeys
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.request.currentRequestIdValue

internal fun loginAudit(event: AuditEventName, login: String): String = auditLog(
    AuditEvent(
        event,
        listOf(
            logField(StructuredLogKeys.REQUEST_ID, currentRequestIdValue()),
            logField(StructuredLogKeys.ACCOUNT, redactLogValue(login)),
            logField(
                StructuredLogKeys.OUTCOME,
                if (event == AuditEventName.LOGIN_SUCCEEDED) AuditOutcome.SUCCESS.wireName else AuditOutcome.FAILURE.wireName
            )
        )
    )
)
