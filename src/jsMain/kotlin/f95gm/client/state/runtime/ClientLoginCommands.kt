package f95gm.client.state.runtime

import f95gm.client.actions.auth.CredentialCommand

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientLoginCommands {
    fun signIn()
    fun updateCredentials(command: CredentialCommand)
}

internal class ClientLoginCommandsAdapter(private val actions: ClientAuthActionsPort) : ClientLoginCommands {
    override fun signIn() = f95gm.client.actions.auth.signIn(actions)
    override fun updateCredentials(command: CredentialCommand) = f95gm.client.actions.auth.updateCredentials(actions, command)
}
