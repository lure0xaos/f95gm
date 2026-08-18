package f95gm.client.model.settings

internal data class AppSettingsState(
    val theme: AppTheme = AppTheme.DARK,
    val lazyLoadImages: Boolean = true,
    val updateCheckInterval: UpdateCheckInterval = UpdateCheckInterval.FIFTEEN_MINUTES,
    val pageSize: PageSize = PageSize.LARGE,
    val cardSize: CardSize = CardSize.DEFAULT
)
