package f95gm.client.actions.auth

import f95gm.client.data.api.postJson
import f95gm.client.model.auth.LoginRequestBody
import f95gm.client.model.auth.SessionState
import f95gm.client.routing.navigation.myGamesRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.catalog.catalog
import f95gm.client.state.runtime.clientJson
import f95gm.client.state.runtime.scope
import f95gm.client.state.session.credentials
import f95gm.client.state.session.session
import f95gm.domain.api.F95Api
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun signIn() {
    scope.launch {
        val values = credentials.data.first()
        if (values.login.isBlank() || values.password.isBlank()) {
            session.enqueue { it.copy(error = UiMessages.app_enterCredentials()) }
            return@launch
        }
        session.enqueue { SessionState(busy = true) }
        runCatching {
            postJson(F95Api.LOGIN, clientJson.encodeToString(LoginRequestBody(values.login, values.password)))
        }.onSuccess {
            session.enqueue { SessionState(authenticated = true) }
            catalog.enqueue {
                it.copy(
                    page = F95Defaults.FIRST_PAGE,
                    search = F95Defaults.EMPTY,
                    error = F95Defaults.EMPTY
                )
            }
            router.navTo(myGamesRoute)
        }.onFailure { error ->
            session.enqueue { SessionState(error = error.message ?: UiMessages.app_signInFailed()) }
        }
    }
}
