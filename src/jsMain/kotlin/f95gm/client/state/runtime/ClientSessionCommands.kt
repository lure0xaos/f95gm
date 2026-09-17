package f95gm.client.state.runtime

import f95gm.client.actions.auth.signOut as signOutFromAuth

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientSessionCommands {
    fun signOut()
}

internal class ClientSessionCommandsAdapter(private val actions: ClientAuthActionsPort) : ClientSessionCommands {
    override fun signOut() = signOutFromAuth(actions)
}
