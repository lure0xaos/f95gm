package f95gm.server.http.request

import f95gm.domain.api.F95ApiErrorCode
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.state.runtime.upstreamJson

internal sealed interface RequestValidation<out T> {
    data class Valid<T>(val value: T) : RequestValidation<T>
    data class Invalid(
        val code: F95ApiErrorCode,
        val message: String,
        val messageKey: ServerMessageKey? = null,
        val field: String? = null
    ) : RequestValidation<Nothing> {
        companion object {
            fun field(
                code: F95ApiErrorCode,
                message: String,
                messageKey: ServerMessageKey,
                field: String
            ) = Invalid(code, message, messageKey, field)
        }
    }
}

internal inline fun <reified T> decodeJson(body: String): RequestValidation<T> =
    runCatching { RequestValidation.Valid(upstreamJson.decodeFromString<T>(body)) }
        .getOrElse {
            RequestValidation.Invalid(
                F95ApiErrorCode.INVALID_INPUT,
                "Invalid JSON request",
                messageKey = f95gm.server.http.response.ServerMessageKeys.INVALID_JSON_REQUEST
            )
        }
