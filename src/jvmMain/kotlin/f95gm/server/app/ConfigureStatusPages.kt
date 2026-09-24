package f95gm.server.app

import f95gm.domain.api.F95ApiError
import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import f95gm.server.config.logging.logger
import f95gm.server.http.request.requestContext
import f95gm.server.http.response.respondJson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import kotlinx.coroutines.CancellationException

internal fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            if (error is CancellationException) throw error
            val mapped = mapApiException(error)
            if (mapped != null) {
                call.respondApiError(mapped.status, mapped.error)
            } else {
                logger.error(error) {
                    "event=unhandled_request_exception requestId=${
                        runCatching { call.requestContext.requestId.value }.getOrDefault(
                            "unknown"
                        )
                    }"
                }
                call.respondApiError(
                    HttpStatusCode.InternalServerError,
                    F95ApiError("Request failed", F95ApiErrorCode.INTERNAL, F95ApiMessageKey("server.requestFailed"))
                )
            }
        }
    }
}

private suspend fun ApplicationCall.respondApiError(status: HttpStatusCode, error: F95ApiError) {
    respondJson(this, status, error)
}
