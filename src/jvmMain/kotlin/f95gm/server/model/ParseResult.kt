package f95gm.server.model

/** Shared success/failure boundary for external text and JSON parsing. */
internal sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Invalid(
        val reason: String,
        val category: ParseFailureCategory = ParseFailureCategory.INVALID_INPUT
    ) : ParseResult<Nothing>
}

internal enum class ParseFailureCategory { INVALID_INPUT, INVALID_JSON, UNSUPPORTED_SHAPE, INVALID_TRACKING_STATE }
