package f95gm.client.state.runtime

import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.client.actions.settings.selectAppTheme as selectAppThemeFromSettings
import f95gm.client.actions.settings.selectCardSize as selectCardSizeFromSettings
import f95gm.client.actions.settings.selectPageSize as selectPageSizeFromSettings
import f95gm.client.actions.settings.selectUpdateCheckInterval as selectUpdateCheckIntervalFromSettings
import f95gm.client.actions.settings.toggleLazyImageLoading as toggleLazyImageLoadingFromSettings

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientSettingsCommands {
    fun selectAppTheme(selected: AppTheme)
    fun selectCardSize(selected: CardSize)
    fun selectPageSize(selected: PageSize)
    fun selectUpdateCheckInterval(selected: UpdateCheckInterval)
    fun toggleLazyImageLoading()
}

internal class ClientSettingsCommandsAdapter(private val actions: ClientSettingsActionsPort) : ClientSettingsCommands {
    override fun selectAppTheme(selected: AppTheme) = selectAppThemeFromSettings(actions, selected)
    override fun selectCardSize(selected: CardSize) = selectCardSizeFromSettings(actions, selected)
    override fun selectPageSize(selected: PageSize) = selectPageSizeFromSettings(actions, selected)
    override fun selectUpdateCheckInterval(selected: UpdateCheckInterval) = selectUpdateCheckIntervalFromSettings(actions, selected)
    override fun toggleLazyImageLoading() = toggleLazyImageLoadingFromSettings(actions)
}
