package f95gm.server.routes.ports

import f95gm.domain.values.F95SessionId
import f95gm.server.upstream.request.UpstreamRequester
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.http.UpstreamResponse

@JvmInline
internal value class LoginName(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
internal value class LoginPassword(val value: String) {
    init { require(value.isNotBlank()) }
}

internal sealed interface UpstreamLoginResult {
    val session: UpstreamSession

    data class MissingToken(override val session: UpstreamSession) : UpstreamLoginResult

    data class Completed(
        override val session: UpstreamSession,
        val initialResponse: UpstreamResponse,
        val result: UpstreamResponse,
        val verified: UpstreamResponse
    ) : UpstreamLoginResult
}

internal interface UpstreamLoginClient {
    suspend fun authenticate(
        gateway: UpstreamRequester,
        login: LoginName,
        password: LoginPassword,
        browserId: F95SessionId
    ): UpstreamLoginResult
}
