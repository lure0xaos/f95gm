package f95gm.server.model.auth

import f95gm.domain.defaults.F95Defaults
import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val login: String = F95Defaults.EMPTY,
    val password: String = F95Defaults.EMPTY
)
