package f95gm.server.observability

import f95gm.server.kernel.LifecycleResourceName

internal data class ResourceCloseFailureEvent(
    val resourceType: LifecycleResourceName,
    val exceptionType: ExceptionType
) : StructuredEvent {
    override val name: StructuredEventName = StructuredLogEventCatalog.eventName(StructuredLogEventCatalog.Event.RESOURCE_CLOSE_FAILURE)
    override val fields: List<StructuredLogField> = listOf(
        logField(StructuredLogKeys.RESOURCE_TYPE, resourceType.value),
        logField(StructuredLogKeys.EXCEPTION_TYPE, exceptionType.value)
    )
}
