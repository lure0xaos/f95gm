package f95gm.client.data.api

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.domain.api.ApiErrorDetailsContract
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

internal data class ParsedApiError(
    val message: String,
    val code: F95ApiErrorCode?,
    val messageKey: F95ApiMessageKey?,
    val details: ApiErrorDetailsContract
)

internal fun parseApiError(body: String, fallback: String): ParsedApiError = try {
    Json.decodeFromString<F95ApiError>(body)
        .let { ParsedApiError(it.message, it.code, it.messageKey, it.typedDetails) }
} catch (_: SerializationException) {
    ParsedApiError(fallback, null, null, ApiErrorDetailsContract())
}

internal fun readApiError(body: String, fallback: String): String = parseApiError(body, fallback).message
