package f95gm.client.state.settings

import f95gm.domain.api.parseNonNegativeLong
import f95gm.domain.api.parseIntOrNull

import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval

internal object SettingsStorageCodec {
    fun encode(value: Boolean): String = value.toString()
    fun decodeBoolean(value: String?, fallback: Boolean): Boolean =
        value?.toBooleanStrictOrNull() ?: fallback

    fun encode(value: AppTheme): String = value.bootstrapValue
    fun decodeTheme(value: String?) = AppTheme.fromValue(value)

    fun encode(value: CardSize): String = value.value
    fun decodeCardSize(value: String?) = CardSize.fromValue(value)

    fun encode(value: PageSize): String = value.value.toString()
    fun decodePageSize(value: String?) = PageSize.fromValue(value?.parseIntOrNull())

    fun encode(value: UpdateCheckInterval): String = value.duration.inWholeMilliseconds.toString()
    fun decodeUpdateCheckInterval(value: String?) = UpdateCheckInterval.fromMillis(value?.parseNonNegativeLong())
}
