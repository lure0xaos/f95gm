package f95gm.server.routes.item

import io.ktor.http.*

internal sealed interface MediaContentTypeResult {
    data class Valid(val value: ContentType) : MediaContentTypeResult
    data object Invalid : MediaContentTypeResult
}

internal fun parseMediaContentType(raw: String): MediaContentTypeResult =
    try {
        MediaContentTypeResult.Valid(ContentType.parse(raw))
    } catch (_: BadContentTypeFormatException) {
        MediaContentTypeResult.Invalid
    }
