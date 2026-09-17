package f95gm.client.actions.auth

import f95gm.client.actions.catalog.loading.cancelCatalogLoad
import f95gm.client.actions.marks.loading.cancelMarkedGamesCheck
import f95gm.client.actions.marks.version.stopVersionCheckLoop
import f95gm.client.data.api.apiResult
import f95gm.client.routing.navigation.loginRoute
import f95gm.client.state.runtime.ClientAuthActionsPort
import f95gm.client.state.runtime.launchAuthenticationOperation

internal fun signOut(runtime: ClientAuthActionsPort) {
    runtime.launchAuthenticationOperation {
        stopVersionCheckLoop(runtime)
        cancelMarkedGamesCheck(runtime)
        cancelCatalogLoad(runtime)
        apiResult { runtime.sessionApi.logoutTyped().body }
        AuthEffects.signedOut(runtime)
        runtime.sessionLifecycle.resetSessionLifecycle()
        runtime.uiEffects.navigate(loginRoute)
    }
}
