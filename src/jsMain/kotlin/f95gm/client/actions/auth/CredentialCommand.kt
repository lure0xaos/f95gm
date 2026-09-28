package f95gm.client.actions.auth

internal sealed interface CredentialCommand {
    data class SetLogin(val value: String) : CredentialCommand
    data class SetPassword(val value: String) : CredentialCommand
    data object Clear : CredentialCommand
}
