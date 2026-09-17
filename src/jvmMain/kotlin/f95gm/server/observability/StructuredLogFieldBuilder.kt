package f95gm.server.observability

internal class StructuredLogFieldBuilder {
    private val values = mutableListOf<StructuredLogField>()

    fun text(name: StructuredFieldName, value: String?) { values += logField(name, value) }
    fun integer(name: StructuredFieldName, value: Int) { values += logField(name, value) }
    fun integer(name: StructuredFieldName, value: Long) { values += logField(name, value) }
    fun boolean(name: StructuredFieldName, value: Boolean) { values += logField(name, value) }

    internal fun build(): List<StructuredLogField> {
        val result = values.toList()
        require(result.map(StructuredLogField::name).distinct().size == result.size) {
            "Structured log fields must be unique"
        }
        return result
    }
}

internal fun structuredFields(block: StructuredLogFieldBuilder.() -> Unit): List<StructuredLogField> =
    StructuredLogFieldBuilder().apply(block).build()
