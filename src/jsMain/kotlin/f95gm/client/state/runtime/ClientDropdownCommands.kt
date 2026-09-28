package f95gm.client.state.runtime

import f95gm.client.state.dropdown.DropdownSearchKey

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientDropdownCommands {
    fun updateDropdownSearch(key: DropdownSearchKey, query: String)
}

internal class ClientDropdownCommandsAdapter(private val actions: ClientDropdownActionsPort) : ClientDropdownCommands {
    override fun updateDropdownSearch(key: DropdownSearchKey, query: String) = f95gm.client.actions.dropdown.updateDropdownSearch(actions, key, query)
}
