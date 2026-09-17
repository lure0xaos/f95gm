package f95gm.client.actions.auth

import f95gm.client.model.auth.SessionState
import f95gm.client.actions.effects.*
import f95gm.client.routing.navigation.myGamesRoute
import f95gm.client.state.runtime.ClientAuthActionsPort
import f95gm.domain.defaults.F95Defaults
import f95gm.client.state.runtime.AsyncState
import f95gm.client.model.runtime.CatalogSearchQuery

/** Keeps state, catalog reset, and navigation effects out of authentication transport code. */
internal object AuthEffects {
    suspend fun authenticated(runtime: ClientAuthActionsPort) {
        runtime.session.enqueue { SessionState(authenticated = true) }
        runtime.catalog.enqueue {
            it.copy(page = F95Defaults.FIRST_PAGE, search = CatalogSearchQuery.EMPTY, async = AsyncState.Success(Unit))
        }
        runtime.uiEffects.navigate(myGamesRoute)
    }

    suspend fun failed(runtime: ClientAuthActionsPort, message: String) {
        runtime.session.enqueue { SessionState(async = AsyncState.Failure(message)) }
    }

    suspend fun signedOut(runtime: ClientAuthActionsPort) {
        runtime.session.enqueue { SessionState() }
        CatalogEffects.reset(runtime.catalog, runtime.catalogOptions)
        FilterEffects.reset(runtime.savedFilters, runtime.savedFilterName)
        DetailEffects.reset(runtime.detail)
        MarkEffects.reset(runtime.marks)
    }

    suspend fun sessionRestored(runtime: ClientAuthActionsPort, authenticated: Boolean) {
        runtime.session.enqueue { SessionState(authenticated = authenticated, async = AsyncState.Success(Unit)) }
    }
}
