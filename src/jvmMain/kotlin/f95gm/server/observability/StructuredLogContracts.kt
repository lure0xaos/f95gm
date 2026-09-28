package f95gm.server.observability

internal interface StructuredLogValue {
    val text: String
    val kind: StructuredLogValueKind

    data class Text(override val text: String) : StructuredLogValue { override val kind = StructuredLogValueKind.TEXT }
    data class Integer(override val text: String) : StructuredLogValue { override val kind = StructuredLogValueKind.INTEGER }
    data class Decimal(override val text: String) : StructuredLogValue { override val kind = StructuredLogValueKind.DECIMAL }
    data class BooleanValue(override val text: String) : StructuredLogValue { override val kind = StructuredLogValueKind.BOOLEAN }
}

internal enum class StructuredLogValueKind { TEXT, INTEGER, DECIMAL, BOOLEAN }

@JvmInline
internal value class StructuredFieldName(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
internal value class StructuredEventName(val value: String) {
    init { require(value.isNotBlank()) }
}

internal data class StructuredLogField(val name: StructuredFieldName, val value: StructuredLogValue?)

internal interface StructuredEvent {
    val name: StructuredEventName
    val fields: List<StructuredLogField>
    val correlationId: String? get() = null
}

internal fun interface StructuredLogSink {
    fun render(event: StructuredEvent): String
}
