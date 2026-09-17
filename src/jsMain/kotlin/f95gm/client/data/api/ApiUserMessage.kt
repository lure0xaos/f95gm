package f95gm.client.data.api

import f95gm.messages.UiMessages
import f95gm.domain.api.F95ApiMessageKey
import f95gm.domain.api.F95ApiMessageKeys

private val apiMessageHandlers: Map<F95ApiMessageKey, (ApiException) -> String> = mapOf(
    F95ApiMessageKeys.AUTHENTICATION_REQUIRED to { UiMessages.server_authenticationRequired() },
    F95ApiMessageKeys.SESSION_EXPIRED to { UiMessages.server_sessionExpired() },
    F95ApiMessageKeys.SIGN_IN_AGAIN to { UiMessages.server_signInAgain() },
    F95ApiMessageKeys.CREDENTIALS_REQUIRED to { UiMessages.server_credentialsRequired() },
    F95ApiMessageKeys.LOGIN_FAILED to { error -> UiMessages.server_loginFailed(error.status?.value?.toString() ?: "?") },
    F95ApiMessageKeys.INVALID_JSON_REQUEST to { UiMessages.server_invalidJsonRequest() },
    F95ApiMessageKeys.INVALID_REQUEST to { UiMessages.server_invalidRequest() },
    F95ApiMessageKeys.REQUEST_FAILED to { UiMessages.server_requestFailed() },
    F95ApiMessageKeys.UPSTREAM_FAILURE to { UiMessages.server_upstreamFailure() },
    F95ApiMessageKeys.INVALID_GAME_ID to { UiMessages.server_invalidGameId() },
    F95ApiMessageKeys.INVALID_TRACKING_STATE to { UiMessages.server_invalidGameTrackingState() },
    F95ApiMessageKeys.INVALID_ITEM_ID to { UiMessages.server_invalidItemId() },
    F95ApiMessageKeys.INVALID_SAVED_FILTER_ID to { UiMessages.server_invalidSavedFilterId() },
    F95ApiMessageKeys.MEDIA_URL_NOT_ALLOWED to { UiMessages.server_mediaUrlNotAllowed() },
    F95ApiMessageKeys.MEDIA_STATUS to { error ->
        error.details.status?.toString()?.let { status -> UiMessages.server_mediaStatus(status) }
            ?: UiMessages.server_mediaStatus(error.status?.value?.toString() ?: "?")
    }
)

internal fun Throwable.apiUserMessage(fallback: String): String {
    val error = this as? ApiException
    return error?.messageKey?.let { key -> apiMessageHandlers[key]?.invoke(error) }
        ?: error?.message ?: message ?: fallback
}

internal fun Throwable.apiUserMessage(fallback: ApiFallback): String =
    apiUserMessage(fallback.message((this as? ApiException)?.status))

internal fun ApiFailure.apiUserMessage(fallback: ApiFallback): String = userMessage(fallback)
