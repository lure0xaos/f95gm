package f95gm.client.state.runtime

import f95gm.client.actions.auth.CredentialCommand
import f95gm.client.actions.auth.signIn as signInFromAuth
import f95gm.client.actions.auth.updateCredentials as updateCredentialsFromAuth

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientLoginCommands {
    fun signIn()
    fun updateCredentials(command: CredentialCommand)
}

internal class ClientLoginCommandsAdapter(private val actions: ClientAuthActionsPort) : ClientLoginCommands {
    override fun signIn() = signInFromAuth(actions)
    override fun updateCredentials(command: CredentialCommand) = updateCredentialsFromAuth(actions, command)
}
