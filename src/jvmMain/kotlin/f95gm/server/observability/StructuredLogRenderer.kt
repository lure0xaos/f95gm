package f95gm.server.observability

internal fun logField(name: StructuredFieldName, value: String?): StructuredLogField =
    StructuredLogField(name, value?.let(StructuredLogValue::Text))
internal fun logField(name: StructuredFieldName, value: Int): StructuredLogField =
    StructuredLogField(name, StructuredLogValue.Integer(value.toString()))
internal fun logField(name: StructuredFieldName, value: Long): StructuredLogField =
    StructuredLogField(name, StructuredLogValue.Integer(value.toString()))
internal fun logField(name: StructuredFieldName, value: f95gm.server.model.http.ResponseBodyBytes): StructuredLogField =
    logField(name, value.value)
internal fun logField(name: StructuredFieldName, value: kotlin.time.Duration?): StructuredLogField =
    value?.let { logField(name, it.inWholeMilliseconds) } ?: logField(name, null as String?)
internal fun logField(name: StructuredFieldName, value: RetryAttempt): StructuredLogField = logField(name, value.value)
internal fun logField(name: StructuredFieldName, value: RequestDurationMillis): StructuredLogField = logField(name, value.value)
internal fun logField(name: StructuredFieldName, value: Boolean): StructuredLogField =
    StructuredLogField(name, StructuredLogValue.BooleanValue(value.toString()))
internal fun logField(name: StructuredFieldName, value: Double): StructuredLogField =
    StructuredLogField(name, StructuredLogValue.Decimal(value.toString()))

internal fun structuredLogText(event: StructuredEventName, fields: Iterable<StructuredLogField>): String = buildString {
    append("event=").append(escape(event.value))
    StructuredLogSchema.allowedFields(event, fields).forEach { field ->
        append(' ').append(escape(field.name.value)).append('=')
            .append(escape(redactLogValue(field.value?.text ?: "-")))
    }
}

internal fun structuredLogText(event: String, fields: Iterable<StructuredLogField>): String =
    structuredLogText(StructuredEventName(event), fields)

internal fun structuredLog(event: String, fields: Iterable<StructuredLogField>): String =
    structuredLogText(event, fields)

internal fun structuredLog(event: StructuredEvent, sink: StructuredLogSink = StructuredLogSinks.text): String = buildString {
    StructuredLogSchema.validate(event)
    append(sink.render(event))
    val correlationId = event.correlationId
        ?: event.fields.firstOrNull { it.name == StructuredLogKeys.REQUEST_ID }?.value?.text
    correlationId?.let {
        append(" correlationId=").append(escape(redactLogValue(it)))
    }
}

internal fun auditLog(event: AuditEvent): String = structuredLog(event)

private fun escape(value: String): String = buildString(value.length) {
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            ' ' -> append("\\s")
            '=' -> append("\\=")
            else -> append(character)
        }
    }
}
