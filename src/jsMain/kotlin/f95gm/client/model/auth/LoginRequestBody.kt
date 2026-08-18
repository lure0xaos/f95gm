package f95gm.client.model.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequestBody(val login: String, val password: String)
