package f95gm.client.actions.effects

import f95gm.client.model.feedback.ToastState
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.runtime.ClientToastPort

/** UI side effects kept outside reducers and data-oriented actions. */
internal interface ClientUiEffects {
    fun navigate(route: TypedRoute)
    suspend fun toast(message: String)
}

internal class RouterClientUiEffects(private val runtime: ClientToastPort) : ClientUiEffects {
    override fun navigate(route: TypedRoute) {
        router.navTo(route)
    }

    override suspend fun toast(message: String) {
        runtime.toast.enqueue { ToastState(message) }
    }
}
