package f95gm.client.actions.auth

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.auth.SessionState
import f95gm.client.state.runtime.ClientAuthActionsPort
import f95gm.client.state.runtime.launchAuthenticationOperation
import f95gm.domain.api.LoginRequestContract
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import f95gm.client.state.runtime.AsyncState

internal fun signIn(runtime: ClientAuthActionsPort) {
    runtime.launchAuthenticationOperation {
        val values = runtime.credentials.data.first()
        if (values.login.isBlank() || values.password.isBlank()) {
            runtime.session.enqueue { it.copy(async = AsyncState.Failure(UiMessages.app_enterCredentials())) }
            return@launchAuthenticationOperation
        }
        runtime.session.enqueue { SessionState(async = AsyncState.Loading) }
        apiResult {
            runtime.sessionApi.loginTyped(LoginRequestContract(values.login, values.password)).body
        }.onSuccess {
            AuthEffects.authenticated(runtime)
        }.onFailure { error ->
            AuthEffects.failed(runtime, error.apiUserMessage(ApiFallbacks.SignIn))
        }
    }
}
