package f95gm.server.routes.session

import f95gm.domain.api.F95ApiErrorCode
import f95gm.domain.values.F95SessionId
import f95gm.messages.UiMessages
import f95gm.server.model.ParseResult
import f95gm.server.http.request.receiveJsonResult
import f95gm.server.http.request.requestField
import f95gm.server.http.response.*
import f95gm.server.model.auth.LoginRequest
import f95gm.server.model.http.OkResponse
import f95gm.server.routes.session.loginDependencies
import f95gm.server.routes.ports.LoginName
import f95gm.server.routes.ports.LoginPassword
import io.ktor.http.*
import io.ktor.server.application.*
import kotlin.uuid.Uuid
import f95gm.server.config.constants.HttpStatusCatalog

internal suspend fun login(call: ApplicationCall) {
    val received = call.receiveJsonResult<LoginRequest>(call.loginDependencies.maxRequestBodyBytes.value)
    val body = received.body
    val jsonResult = received.result
    val jsonRequest = (jsonResult as? ParseResult.Success)?.value
    if (body.trimStart().startsWith("{") && jsonResult is ParseResult.Invalid) {
        respondFailure(
            call,
            HttpStatusCatalog.BAD_REQUEST,
            ApiFailure("Invalid JSON request", ServerMessageKey.INVALID_JSON_REQUEST, F95ApiErrorCode.INVALID_INPUT)
        )
        return
    }
    val login = (jsonRequest?.login ?: requestField(body, "login")).orEmpty().trim()
    val password = (jsonRequest?.password ?: requestField(body, "password")).orEmpty()
    if (login.isBlank() || password.isBlank()) {
        respondFailure(
            call,
            HttpStatusCatalog.BAD_REQUEST,
            ApiFailure(UiMessages.server_credentialsRequired(), ServerMessageKey.CREDENTIALS_REQUIRED)
        )
        return
    }

    val browserId = F95SessionId(Uuid.random().toString())
    val dependencies = call.loginDependencies
    when (val result = dependencies.loginService.authenticate(LoginName(login), LoginPassword(password), browserId)) {
        is LoginApplicationResult.Rejected -> respondFailure(call, result.response())
        is LoginApplicationResult.Authenticated -> {
            setBrowserCookie(call, result.browserId)
            respondJson(call, HttpStatusCatalog.OK, OkResponse(true))
        }
    }
}

