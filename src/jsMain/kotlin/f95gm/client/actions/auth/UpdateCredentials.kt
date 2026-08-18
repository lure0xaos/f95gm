package f95gm.client.actions.auth

import f95gm.client.state.runtime.scope
import f95gm.client.state.session.credentials
import kotlinx.coroutines.launch

internal fun updateCredentials(login: String? = null, password: String? = null) {
    scope.launch {
        credentials.enqueue { it.copy(login = login ?: it.login, password = password ?: it.password) }
    }
}
