package f95gm.client.state.runtime


/** UI commands expose intent without writable stores or transport access. */
internal interface ClientSessionCommands {
    fun signOut()
}

internal class ClientSessionCommandsAdapter(private val actions: ClientAuthActionsPort) : ClientSessionCommands {
    override fun signOut() = f95gm.client.actions.auth.signOut(actions)
}
