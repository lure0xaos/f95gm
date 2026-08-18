package f95gm.server.model.http

import kotlinx.serialization.Serializable

@Serializable
internal data class JsonMessage(val message: String)
