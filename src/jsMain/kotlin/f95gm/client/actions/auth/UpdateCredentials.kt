package f95gm.client.actions.auth

import f95gm.client.state.runtime.ClientAuthActionsPort
import kotlinx.coroutines.launch

internal fun updateCredentials(runtime: ClientAuthActionsPort, command: CredentialCommand) {
    runtime.launchEffect {
        runtime.credentials.enqueue { current ->
            when (command) {
                is CredentialCommand.SetLogin -> current.copy(login = command.value)
                is CredentialCommand.SetPassword -> current.copy(password = command.value)
                CredentialCommand.Clear -> current.copy(login = "", password = "")
            }
        }
    }
}
