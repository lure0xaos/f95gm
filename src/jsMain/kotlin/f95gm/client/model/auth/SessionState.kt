package f95gm.client.model.auth

internal data class SessionState(
    val authenticated: Boolean = false,
    val busy: Boolean = false,
    val error: String = "",
    val checking: Boolean = false
)
