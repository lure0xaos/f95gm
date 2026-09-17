package f95gm.domain.api

/** Parsed route template shared by endpoint validation and runtime matching. */
internal data class ApiPathTemplate private constructor(
    val segments: List<Segment>
) {
    internal sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Parameter(val name: String) : Segment
    }

    fun matches(path: String): Boolean {
        val values = normalize(path).splitSegments()
        return values.size == segments.size && segments.zip(values).all { (segment, value) ->
            when (segment) {
                is Segment.Literal -> segment.value == value
                is Segment.Parameter -> true
            }
        }
    }

    companion object {
        fun parse(template: String): ApiPathTemplate = ApiPathTemplate(
            normalize(template).splitSegments().map { segment ->
                if (segment.startsWith("{") && segment.endsWith("}")) {
                    Segment.Parameter(segment.substring(1, segment.length - 1))
                } else {
                    Segment.Literal(segment)
                }
            }
        )

        private fun normalize(path: String): String =
            path.substringBefore('?').let { if (it.length > 1) it.trimEnd('/') else it }
    }
}

private fun String.splitSegments(): List<String> = trim('/').let { value ->
    if (value.isEmpty()) emptyList() else value.split('/')
}
