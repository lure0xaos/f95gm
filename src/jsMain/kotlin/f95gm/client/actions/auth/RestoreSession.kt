package f95gm.client.actions.auth

import f95gm.client.data.api.getJson
import f95gm.client.model.auth.SessionState
import f95gm.client.state.session.session
import f95gm.domain.api.F95Api

internal suspend fun restoreSession() {
    runCatching { getJson(F95Api.SESSION) }
        .onSuccess {
            session.enqueue { SessionState(authenticated = true) }
        }
        .onFailure {
            session.enqueue { SessionState() }
        }
}
