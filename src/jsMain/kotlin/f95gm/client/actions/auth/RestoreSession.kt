package f95gm.client.actions.auth

import f95gm.client.data.api.apiResult
import f95gm.client.state.runtime.ClientAuthActionsPort

internal suspend fun restoreSession(runtime: ClientAuthActionsPort) {
    apiResult { runtime.sessionApi.sessionTyped().body }
        .onSuccess { response ->
            AuthEffects.sessionRestored(runtime, response.ok)
        }
        .onFailure {
            AuthEffects.sessionRestored(runtime, false)
        }
}
