package f95gm.server.http.request

import f95gm.domain.api.ApiQueryParameter
import io.ktor.http.Parameters
import io.ktor.server.request.ApplicationRequest
import f95gm.domain.api.parseIntOrNull

/** Keeps the canonical query-parameter name typed until the Ktor string boundary. */
internal operator fun Parameters.get(parameter: ApiQueryParameter): String? = get(parameter.value)

internal fun ApplicationRequest.valuesParameter(parameter: ApiQueryParameter): List<String> =
    queryParameters.entries()
        .filter { (name, _) -> name == parameter.value || name == "${parameter.value}[]" }
        .flatMap { (_, values) -> values }

internal fun ApplicationRequest.textParameter(parameter: ApiQueryParameter, maxLength: Int): String =
    queryParameters[parameter].orEmpty().trim().take(maxLength)

internal fun ApplicationRequest.requiredTextParameter(parameter: ApiQueryParameter, maxLength: Int): String? =
    queryParameters[parameter]?.trim()?.takeIf { it.isNotEmpty() }?.take(maxLength)

internal fun ApplicationRequest.allowedTextParameter(
    parameter: ApiQueryParameter,
    default: String,
    allowed: Collection<String>
): String = queryParameters[parameter]?.trim()?.takeIf { it in allowed } ?: default

internal fun ApplicationRequest.intParameter(
    parameter: ApiQueryParameter,
    default: Int,
    range: IntRange
): Int = queryParameters[parameter]?.trim()?.parseIntOrNull()?.takeIf(range::contains) ?: default

internal fun ApplicationRequest.oneOfIntParameter(
    parameter: ApiQueryParameter,
    default: Int,
    allowed: Collection<Int>
): Int = queryParameters[parameter]?.trim()?.parseIntOrNull()?.takeIf { it in allowed } ?: default

internal fun <T> ApplicationRequest.enumParameter(
    parameter: ApiQueryParameter,
    default: T,
    values: Iterable<T>,
    wire: (T) -> String
): T = queryParameters[parameter]?.trim()?.let { value -> values.firstOrNull { wire(it) == value } } ?: default
