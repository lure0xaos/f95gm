package f95gm.server.http.request

import f95gm.server.model.ParseResult
import f95gm.server.model.ParseFailureCategory
import f95gm.server.http.serialization.upstreamJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement

internal inline fun <reified T> decodeJsonResult(body: String): ParseResult<T> =
    try {
        ParseResult.Success(upstreamJson.decodeFromString<T>(body))
    } catch (_: SerializationException) {
        ParseResult.Invalid("Invalid JSON", ParseFailureCategory.INVALID_JSON)
    } catch (_: IllegalArgumentException) {
        ParseResult.Invalid("Invalid JSON", ParseFailureCategory.INVALID_JSON)
    }

internal fun parseJsonElementResult(body: String): ParseResult<JsonElement> = try {
    ParseResult.Success(upstreamJson.parseToJsonElement(body))
} catch (_: SerializationException) {
    ParseResult.Invalid("Invalid JSON", ParseFailureCategory.INVALID_JSON)
} catch (_: IllegalArgumentException) {
    ParseResult.Invalid("Invalid JSON", ParseFailureCategory.INVALID_JSON)
}

/** Compatibility helper for optional metadata where invalid JSON means absent metadata. */
internal fun parseJsonElementOrNull(body: String): JsonElement? =
    when (val result = parseJsonElementResult(body)) {
        is ParseResult.Success -> result.value
        is ParseResult.Invalid -> null
    }

