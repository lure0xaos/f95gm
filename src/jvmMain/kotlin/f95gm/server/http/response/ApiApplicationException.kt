package f95gm.server.http.response

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95ApiMessageKey
import io.ktor.http.*

internal open class ApiApplicationException(
    val status: HttpStatusCode,
    message: String,
    val code: F95ApiErrorCode = F95ApiErrorCode.INTERNAL,
    val messageKey: F95ApiMessageKey? = null,
    val details: Map<String, String> = emptyMap()
) : RuntimeException(message)
