package f95gm.server.http.request

import io.ktor.server.request.*

internal fun ApplicationRequest.textParameter(name: String, maxLength: Int): String =
    queryParameters[name]?.trim()?.take(maxLength).orEmpty()

internal fun ApplicationRequest.allowedTextParameter(
    name: String,
    default: String,
    allowed: Collection<String>
): String =
    queryParameters[name]?.takeIf { it in allowed } ?: default

internal fun ApplicationRequest.oneOfIntParameter(name: String, default: Int, allowed: Collection<Int>): Int =
    queryParameters[name]?.toIntOrNull()?.takeIf { it in allowed } ?: default

internal fun ApplicationRequest.intParameter(name: String, default: Int, range: IntRange): Int =
    queryParameters[name]?.toIntOrNull()?.takeIf { it in range } ?: default

internal fun <T> ApplicationRequest.enumParameter(
    name: String,
    default: T,
    values: Collection<T>,
    wire: (T) -> String = { it.toString() }
): T = queryParameters[name]?.let { value -> values.firstOrNull { wire(it) == value } } ?: default

internal fun ApplicationRequest.requiredTextParameter(name: String, maxLength: Int): String? =
    queryParameters[name]?.trim()?.takeIf { it.isNotBlank() }?.take(maxLength)
