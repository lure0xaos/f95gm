package f95gm.server.observability

internal object StructuredLogSchema {
    typealias StructuredField = StructuredLogEventCatalog.Field

    /** Single authoritative schema consumed by renderers and compatibility checks. */
    val events: Map<StructuredEventName, Set<StructuredField>> = StructuredLogEventCatalog.events
    val names: Set<String> = events.keys.mapTo(mutableSetOf(), StructuredEventName::value)

    fun validate(event: StructuredEvent) = StructuredLogSchemaPolicy.validate(events, event)
    fun validate(name: StructuredEventName, fields: List<StructuredLogField>) =
        StructuredLogSchemaPolicy.validate(events, name, fields)

    fun allowedFields(name: StructuredEventName, fields: Iterable<StructuredLogField>): List<StructuredLogField> {
        val values = fields.toList()
        validate(name, values.filter { isAllowedStructuredField(name, it.name) })
        return values.filter { isAllowedStructuredField(name, it.name) }
    }

    init { StructuredLogSchemaPolicy.validate(events) }
}

internal fun allowedStructuredFields(event: String): Set<String> =
    StructuredLogSchema.events[StructuredEventName(event)].orEmpty().mapTo(mutableSetOf()) { it.wireName }

internal fun isAllowedStructuredField(event: StructuredEventName, field: StructuredFieldName): Boolean =
    StructuredLogSchema.events[event].orEmpty().any { it.wireName == field.value }
