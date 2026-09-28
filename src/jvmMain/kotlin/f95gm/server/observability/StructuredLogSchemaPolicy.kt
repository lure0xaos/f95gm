package f95gm.server.observability

import f95gm.server.kernel.ServerInvariantViolation

/** Validates the bounded field contract independently from schema declarations. */
internal object StructuredLogSchemaPolicy {
    fun validate(events: Map<StructuredEventName, Set<StructuredLogEventCatalog.Field>>) {
        check(events.size == events.keys.size) { "Structured log event names must be unique" }
        check(events.values.all { it.isNotEmpty() }) { "Every structured log event must declare at least one field" }
    }

    fun validate(
        events: Map<StructuredEventName, Set<StructuredLogEventCatalog.Field>>,
        event: StructuredEvent
    ) = validate(events, event.name, event.fields)

    fun validate(
        events: Map<StructuredEventName, Set<StructuredLogEventCatalog.Field>>,
        name: StructuredEventName,
        fields: List<StructuredLogField>
    ) {
        val allowed = events[name]
            ?: throw ServerInvariantViolation("Unknown structured log event: ${name.value}")
        val fieldNames = fields.map(StructuredLogField::name)
        require(fieldNames.size == fieldNames.toSet().size) {
            "Structured log event ${name.value} contains duplicate fields"
        }
        require(fieldNames.size <= allowed.size) {
            "Structured log event ${name.value} exceeds its bounded field cardinality"
        }
        val invalid = fieldNames.filterNot { field -> allowed.any { it.wireName == field.value } }
        require(invalid.isEmpty()) {
            "Structured log event ${name.value} contains undeclared fields: ${invalid.map { it.value }}"
        }
        require(fields.all {
            it.value == null || it.value.kind in StructuredLogEventCatalog.allowedValueKinds(name, it.name)
        }) {
            "Structured log event ${name.value} contains a value with the wrong type"
        }
    }
}
