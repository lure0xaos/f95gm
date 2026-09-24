package f95gm.server.http.response

import f95gm.domain.api.F95ApiMessageKey

internal enum class ServerMessageKey(val value: String) {
    AUTHENTICATION_REQUIRED("server.authenticationRequired"),
    INVALID_GAME_ID("server.invalidGameId"),
    INVALID_ITEM_ID("server.invalidItemId"),
    INVALID_FILTER_ID("server.invalidSavedFilterId"),
    INVALID_BACKUP("server.invalidQuickLinksBackup"),
    MEDIA_URL_NOT_ALLOWED("server.mediaUrlNotAllowed"),
    MEDIA_STATUS("server.mediaStatus"),
    CATALOG_OPTIONS_MISSING("server.catalogOptionsMissing"),
    CATALOG_OPTIONS_INVALID("server.catalogOptionsInvalid"),
    SIGN_IN_AGAIN("server.signInAgain"),
    CREDENTIALS_REQUIRED("server.credentialsRequired"),
    LOGIN_TOKEN_MISSING("server.loginTokenMissing"),
    LOGIN_FAILED("server.loginFailed"),
    SESSION_MISSING("server.noActiveSession"),
    SESSION_EXPIRED("server.sessionExpired"),
    INVALID_JSON_REQUEST("server.invalidJsonRequest"),
    INVALID_TRACKING_STATE("server.invalidTrackingState"),
    SAVED_FILTER_NAME_REQUIRED("server.savedFilterNameRequired"),
    MARK_CONFLICT("server.markConflict"),
    SAVED_FILTER_CONFLICT("server.savedFilterConflict"),
    MUTATION_IN_PROGRESS("server.mutationInProgress"),
    IDEMPOTENCY_KEY_REUSED("server.idempotencyKeyReused"),
    INVALID_ITEM_LOAD("server.itemLoadFailed"),
    UPSTREAM_FAILURE("server.upstreamFailure");


    val apiKey: F95ApiMessageKey get() = F95ApiMessageKey(value)
}
