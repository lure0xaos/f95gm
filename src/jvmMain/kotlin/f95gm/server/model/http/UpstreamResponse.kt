package f95gm.server.model.http

internal data class UpstreamResponse(
    val status: Int,
    val body: String,
    val contentType: String?,
    val location: String? = null
)
