package f95gm.server.observability

import f95gm.server.kernel.FailureCategory

internal data class FailureReportedEvent(
    val scope: FailureScope,
    val category: FailureCategory,
    val exceptionType: ExceptionType
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.FAILURE_REPORTED)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.SCOPE, scope.value),
        logField(StructuredLogKeys.CATEGORY, category.name.lowercase()),
        logField(StructuredLogKeys.EXCEPTION_TYPE, exceptionType.value)
    )
}
