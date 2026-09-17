package f95gm.server.http.request

import f95gm.server.model.ParseResult
import io.ktor.server.application.ApplicationCall

internal data class ReceiveJsonResult<T>(val body: String, val result: ParseResult<T>)

internal suspend inline fun <reified T> ApplicationCall.receiveJsonResult(maxBytes: Long): ReceiveJsonResult<T> {
    val body = receiveLimitedText(maxBytes)
    return ReceiveJsonResult(body, decodeJsonResult(body))
}

