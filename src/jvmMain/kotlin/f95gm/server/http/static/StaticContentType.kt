package f95gm.server.http.static

import io.ktor.http.*

internal fun staticContentType(extension: String): ContentType = when (extension.lowercase()) {
    "html" -> ContentType.Text.Html
    "js" -> ContentType.Application.JavaScript
    "css" -> ContentType.Text.CSS
    "map" -> ContentType.Application.Json
    "png" -> ContentType.Image.PNG
    else -> ContentType.Application.OctetStream
}
