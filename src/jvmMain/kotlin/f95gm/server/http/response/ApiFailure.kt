package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.server.config.constants.HttpStatusCatalog
import io.ktor.http.*
import io.ktor.server.application.*

internal data class ApiFailure(
    val message: String,
    val key: ServerMessageKey,
    val code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    val details: ApiFailureDetails = ApiFailureDetails.EMPTY
) {
    companion object {
        fun invalidInput(message: String, key: ServerMessageKey, details: ApiFailureDetails = ApiFailureDetails.EMPTY) =
            ApiFailure(message, key, F95ApiErrorCode.INVALID_INPUT, details)

        fun authenticationRequired(message: String, key: ServerMessageKey) =
            ApiFailure(message, key)
    }
}

internal data class ApiFailureResponse(val status: HttpStatusCode, val failure: ApiFailure)

internal fun interface ApiResponsePolicy<in T> {
    fun failure(value: T): ApiFailureResponse
}

internal fun badRequestResponse(failure: ApiFailure): ApiFailureResponse =
    ApiFailureResponse(HttpStatusCatalog.BAD_REQUEST, failure)

internal suspend fun respondFailure(call: ApplicationCall, status: HttpStatusCode, failure: ApiFailure) {
    respond(call, status, jsonMessage(failure))
}

internal suspend fun respondFailure(call: ApplicationCall, response: ApiFailureResponse) =
    respondFailure(call, response.status, response.failure)

internal suspend fun respondApiFailure(
    call: ApplicationCall,
    status: HttpStatusCode,
    message: String,
    key: ServerMessageKey,
    code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    details: ApiFailureDetails = ApiFailureDetails.EMPTY
) = respondFailure(call, status, ApiFailure(message, key, code, details))

internal suspend fun respondApiFailure(call: ApplicationCall, status: HttpStatusCode, failure: ApiFailure) =
    respondFailure(call, status, failure)
