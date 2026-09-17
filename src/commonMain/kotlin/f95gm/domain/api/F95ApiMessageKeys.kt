package f95gm.domain.api

/** Canonical server message keys shared by classification and presentation. */
object F95ApiMessageKeys {
    val AUTHENTICATION_REQUIRED = F95ApiMessageKey("server.authenticationRequired")
    val SESSION_EXPIRED = F95ApiMessageKey("server.sessionExpired")
    val SIGN_IN_AGAIN = F95ApiMessageKey("server.signInAgain")
    val CREDENTIALS_REQUIRED = F95ApiMessageKey("server.credentialsRequired")
    val LOGIN_FAILED = F95ApiMessageKey("server.loginFailed")
    val INVALID_JSON_REQUEST = F95ApiMessageKey("server.invalidJsonRequest")
    val INVALID_REQUEST = F95ApiMessageKey("server.invalidRequest")
    val REQUEST_FAILED = F95ApiMessageKey("server.requestFailed")
    val UPSTREAM_FAILURE = F95ApiMessageKey("server.upstreamFailure")
    val INVALID_GAME_ID = F95ApiMessageKey("server.invalidGameId")
    val INVALID_TRACKING_STATE = F95ApiMessageKey("server.invalidTrackingState")
    val INVALID_ITEM_ID = F95ApiMessageKey("server.invalidItemId")
    val INVALID_SAVED_FILTER_ID = F95ApiMessageKey("server.invalidSavedFilterId")
    val MEDIA_URL_NOT_ALLOWED = F95ApiMessageKey("server.mediaUrlNotAllowed")
    val MEDIA_STATUS = F95ApiMessageKey("server.mediaStatus")

    val AUTHENTICATION = setOf(AUTHENTICATION_REQUIRED, SESSION_EXPIRED, SIGN_IN_AGAIN, CREDENTIALS_REQUIRED, LOGIN_FAILED)
}
