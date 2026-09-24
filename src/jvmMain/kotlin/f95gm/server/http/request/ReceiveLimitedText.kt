package f95gm.server.http.request

import f95gm.server.http.response.ApiRequestTooLargeException
import io.ktor.server.application.*
import io.ktor.server.request.*

internal suspend fun ApplicationCall.receiveLimitedText(maxBytes: Long): String {
    val bytes = try {
        receiveChannel().readLimitedBytes(maxBytes)
    } catch (_: BodyLimitExceededException) {
        throw ApiRequestTooLargeException()
    }
    return bytes.decodeToString()
}
