package f95gm.server.routes.session

import f95gm.messages.UiMessages
import f95gm.server.config.protocol.UpstreamProtocolConfig
import f95gm.server.http.auth.upstreamLoginMessage
import f95gm.server.model.http.UpstreamResponse

internal fun loginFailureMessage(
    result: UpstreamResponse,
    verified: UpstreamResponse,
    verificationState: LoginVerificationState,
    sessionCookieNames: Set<String>,
    initialLocation: String?,
    baseUrl: String
): String {
    val upstreamMessage = upstreamLoginMessage(result.body, baseUrl)
    return when {
        upstreamMessage?.contains("incorrect", ignoreCase = true) == true -> UiMessages.server_credentialsRejected()
        !upstreamMessage.isNullOrBlank() -> upstreamMessage
        verificationState == LoginVerificationState.NOT_VERIFIED && UpstreamProtocolConfig.SESSION_COOKIE !in sessionCookieNames ->
            UiMessages.server_sessionNotConfirmedCookie(initialLocation ?: "none", verified.status)
        verificationState == LoginVerificationState.NOT_VERIFIED -> UiMessages.server_sessionNotConfirmedPage(initialLocation ?: "none", verified.status)
        else -> UiMessages.server_loginFailed(result.status.value)
    }
}
