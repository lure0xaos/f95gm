package f95gm.client.data.api

import f95gm.messages.UiMessages

internal fun Throwable.apiUserMessage(fallback: String): String {
    val error = this as? ApiException
    return when (error?.messageKey) {
        "server.authenticationRequired" -> UiMessages.server_authenticationRequired()
        "server.sessionExpired" -> UiMessages.server_sessionExpired()
        "server.signInAgain" -> UiMessages.server_signInAgain()
        "server.credentialsRequired" -> UiMessages.server_credentialsRequired()
        "server.loginFailed" -> UiMessages.server_loginFailed(error.status?.toString() ?: "?")
        "server.invalidJsonRequest" -> UiMessages.server_invalidJsonRequest()
        "server.invalidRequest" -> UiMessages.server_invalidRequest()
        "server.requestFailed" -> UiMessages.server_requestFailed()
        "server.upstreamFailure" -> UiMessages.server_upstreamFailure()
        "server.invalidGameId" -> UiMessages.server_invalidGameId()
        "server.invalidTrackingState" -> UiMessages.server_invalidGameTrackingState()
        "server.invalidItemId" -> UiMessages.server_invalidItemId()
        "server.invalidSavedFilterId" -> UiMessages.server_invalidSavedFilterId()
        "server.mediaUrlNotAllowed" -> UiMessages.server_mediaUrlNotAllowed()
        "server.mediaStatus" -> error.details["status"]?.let { UiMessages.server_mediaStatus(it) }
            ?: UiMessages.server_mediaStatus(error.status?.toString() ?: "?")

        else -> error?.message ?: message ?: fallback
    }
}
