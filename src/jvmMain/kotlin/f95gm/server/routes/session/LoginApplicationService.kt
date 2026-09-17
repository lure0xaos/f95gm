package f95gm.server.routes.session

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.api.F95HttpStatus
import f95gm.domain.values.F95SessionId
import f95gm.messages.UiMessages
import f95gm.server.observability.*
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.auth.isAuthenticatedUpstreamResponse
import f95gm.server.http.auth.isUnauthenticatedUpstreamBody
import f95gm.server.http.auth.upstreamLoginMessage
import f95gm.server.http.response.ApiFailure
import f95gm.server.http.response.ApiFailureResponse
import f95gm.server.http.response.ApiResponsePolicy
import f95gm.server.http.response.ServerMessageKey
import f95gm.server.http.response.upstreamFailureApiFailure
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse
import f95gm.server.routes.ports.UpstreamLoginResult
import f95gm.server.routes.ports.LoginName
import f95gm.server.routes.ports.LoginPassword
import io.ktor.http.HttpStatusCode
import f95gm.server.config.constants.HttpStatusCatalog

internal sealed interface LoginApplicationResult {
    data class Rejected(val status: HttpStatusCode, val failure: ApiFailure) : LoginApplicationResult
    data class Authenticated(val browserId: F95SessionId, val session: UpstreamSession) : LoginApplicationResult
}

private val loginResponsePolicy = ApiResponsePolicy<LoginApplicationResult.Rejected> { rejected ->
    ApiFailureResponse(rejected.status, rejected.failure)
}

internal fun LoginApplicationResult.Rejected.response(): ApiFailureResponse = loginResponsePolicy.failure(this)

internal class LoginApplicationService(private val dependencies: LoginApplicationDependencies) {
    suspend fun authenticate(login: LoginName, password: LoginPassword, browserId: F95SessionId): LoginApplicationResult {
        val authentication = dependencies.upstreamLoginClient.authenticate(
            dependencies.upstream,
            login,
            password,
            browserId
        )
        val completed = when (authentication) {
            is UpstreamLoginResult.MissingToken -> return LoginApplicationResult.Rejected(
                HttpStatusCatalog.BAD_GATEWAY,
                ApiFailure(UiMessages.server_loginTokenMissing(), ServerMessageKey.LOGIN_TOKEN_MISSING)
            )
            is UpstreamLoginResult.Completed -> authentication
        }
        val result = completed.result
        val verified = completed.verified
        if (result is UpstreamResponse.Failure) {
            return LoginApplicationResult.Rejected(result.status, upstreamFailureApiFailure(result.reason))
        }
        if (verified is UpstreamResponse.Failure) {
            return LoginApplicationResult.Rejected(verified.status, upstreamFailureApiFailure(verified.reason))
        }

        val verificationState = if (isAuthenticatedUpstreamResponse(verified, dependencies.upstreamEndpoints.origin)) {
            LoginVerificationState.VERIFIED
        } else {
            LoginVerificationState.NOT_VERIFIED
        }
        if (result.status.value !in F95HttpStatus.SUCCESS_MIN..399 ||
            verificationState == LoginVerificationState.NOT_VERIFIED
        ) {
            val session = completed.session
            val baseUrl = dependencies.upstreamEndpoints.origin
            val upstreamMessage = upstreamLoginMessage(result.body, baseUrl)
            logger.warn {
                    "F95zone login verification failed: initialStatus=${completed.initialResponse.status}, " +
                    "initialLocation=${completed.initialResponse.location?.value?.substringBefore('?') ?: "none"}, " +
                    "finalStatus=${result.status.value}, verificationStatus=${verified.status}, " +
                    "cookies=${session.cookieNames().joinToString(",")}, " +
                    "finalUnauthenticated=${isUnauthenticatedUpstreamBody(result.body, baseUrl)}, " +
                    "verificationUnauthenticated=${isUnauthenticatedUpstreamBody(verified.body, baseUrl)}, " +
                    "message=${redactLogValue(upstreamMessage ?: "none")}"
            }
            logger.info { loginAudit(AuditEventName.LOGIN_FAILED, login.value) }
            return LoginApplicationResult.Rejected(
                HttpStatusCatalog.UNAUTHORIZED,
                ApiFailure(
                    loginFailureMessage(
                        result,
                        verified,
                        verificationState,
                        session.cookieNames().mapTo(linkedSetOf()) { it.value },
                        completed.initialResponse.location?.value?.substringBefore('?'),
                        baseUrl
                    ),
                    ServerMessageKey.LOGIN_FAILED
                )
            )
        }

        dependencies.sessionService.persistAndRegister(browserId, completed.session)
        logger.info { loginAudit(AuditEventName.LOGIN_SUCCEEDED, login.value) }
        return LoginApplicationResult.Authenticated(browserId, completed.session)
    }
}
