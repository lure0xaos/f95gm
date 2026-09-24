package f95gm.client.data.api

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import kotlinx.serialization.json.Json

internal data class ParsedApiError(
    val message: String,
    val code: F95ApiErrorCode?,
    val messageKey: String?,
    val details: Map<String, String>
)

internal fun parseApiError(body: String, fallback: String): ParsedApiError = runCatching {
    Json.decodeFromString<F95ApiError>(body)
        .let { ParsedApiError(it.message, it.code, it.messageKey?.value, it.details) }
}.getOrElse { ParsedApiError(fallback, null, null, emptyMap()) }

internal fun readApiError(body: String, fallback: String): String = parseApiError(body, fallback).message
