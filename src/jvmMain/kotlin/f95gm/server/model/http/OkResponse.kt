package f95gm.server.model.http

import kotlinx.serialization.Serializable

@Serializable
internal data class OkResponse(val ok: Boolean)
