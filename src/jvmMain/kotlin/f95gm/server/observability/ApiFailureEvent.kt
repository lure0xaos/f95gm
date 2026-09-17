package f95gm.server.observability

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.model.http.HttpStatusValue

internal data class ApiFailureEvent(
    val status: HttpStatusValue,
    val code: F95ApiErrorCode,
    val messageKey: F95ApiMessageKey?,
    val exceptionType: ExceptionType
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.API_FAILURE)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.STATUS, status.value),
        logField(StructuredLogKeys.CODE, code.name.lowercase()),
        logField(StructuredLogKeys.MESSAGE_KEY, messageKey?.value),
        logField(StructuredLogKeys.EXCEPTION_TYPE, exceptionType.value)
    )
}
