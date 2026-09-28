package f95gm.server.app

internal sealed interface AuthenticationResult {
    data object Allowed : AuthenticationResult
    data object ResponseCompleted : AuthenticationResult
}
