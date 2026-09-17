package f95gm.server.http.request

import f95gm.server.http.response.respondFailure
import f95gm.server.config.constants.HttpStatusCatalog
import io.ktor.server.application.ApplicationCall

internal data class ReceiveMutationRequest<T>(val body: String, val request: T)

internal suspend inline fun <reified T> ApplicationCall.receiveMutationRequest(
    maxBytes: Long
): ReceiveMutationRequest<T>? {
    val received = receiveValidatedJson<T>(maxBytes)
    return when (val result = received.result) {
        is RequestValidation.Valid -> ReceiveMutationRequest(received.body, result.value)
        is RequestValidation.Invalid -> {
            respondFailure(this, HttpStatusCatalog.BAD_REQUEST, result.toApiFailure())
            null
        }
    }
}
