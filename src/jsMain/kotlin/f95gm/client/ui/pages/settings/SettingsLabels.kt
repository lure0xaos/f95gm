package f95gm.client.ui.pages.settings

import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.messages.UiMessages

internal fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.DARK -> UiMessages.app_themeDark()
    AppTheme.LIGHT -> UiMessages.app_themeLight()
    AppTheme.SYSTEM -> UiMessages.app_themeSystem()
}

internal fun intervalLabel(interval: UpdateCheckInterval): String = when (interval) {
    UpdateCheckInterval.DISABLED -> UiMessages.app_updatesDisabled()
    UpdateCheckInterval.FIFTEEN_MINUTES -> UiMessages.app_every15Minutes()
    UpdateCheckInterval.HOUR -> UiMessages.app_everyHour()
    UpdateCheckInterval.SIX_HOURS -> UiMessages.app_every6Hours()
    UpdateCheckInterval.DAY -> UiMessages.app_everyDay()
}

internal fun pageSizeLabel(size: PageSize): String = size.value.toString()

internal fun cardSizeLabel(size: CardSize): String = when (size) {
    CardSize.VERY_COMPACT -> UiMessages.app_cardSizeVeryCompact()
    CardSize.COMPACT -> UiMessages.app_cardSizeCompact()
    CardSize.DEFAULT -> UiMessages.app_cardSizeDefault()
    CardSize.LARGE -> UiMessages.app_cardSizeLarge()
    CardSize.VERY_LARGE -> UiMessages.app_cardSizeVeryLarge()
}
