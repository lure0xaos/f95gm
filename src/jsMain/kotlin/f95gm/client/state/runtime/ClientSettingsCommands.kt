package f95gm.client.state.runtime

import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientSettingsCommands {
    fun selectAppTheme(selected: AppTheme)
    fun selectCardSize(selected: CardSize)
    fun selectPageSize(selected: PageSize)
    fun selectUpdateCheckInterval(selected: UpdateCheckInterval)
    fun toggleLazyImageLoading()
}

internal class ClientSettingsCommandsAdapter(private val actions: ClientSettingsActionsPort) : ClientSettingsCommands {
    override fun selectAppTheme(selected: AppTheme) = f95gm.client.actions.settings.selectAppTheme(actions, selected)
    override fun selectCardSize(selected: CardSize) = f95gm.client.actions.settings.selectCardSize(actions, selected)
    override fun selectPageSize(selected: PageSize) = f95gm.client.actions.settings.selectPageSize(actions, selected)
    override fun selectUpdateCheckInterval(selected: UpdateCheckInterval) = f95gm.client.actions.settings.selectUpdateCheckInterval(actions, selected)
    override fun toggleLazyImageLoading() = f95gm.client.actions.settings.toggleLazyImageLoading(actions)
}
