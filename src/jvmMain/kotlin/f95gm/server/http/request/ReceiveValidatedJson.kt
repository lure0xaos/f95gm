package f95gm.server.http.request

import io.ktor.server.application.ApplicationCall

internal data class ReceiveValidatedJson<T>(val body: String, val result: RequestValidation<T>)

internal suspend inline fun <reified T> ApplicationCall.receiveValidatedJson(maxBytes: Long): ReceiveValidatedJson<T> {
    val body = receiveLimitedText(maxBytes)
    return ReceiveValidatedJson(body, decodeJson(body))
}
