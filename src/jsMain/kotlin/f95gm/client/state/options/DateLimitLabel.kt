package f95gm.client.state.options

import f95gm.domain.api.parseIntOrNull
import f95gm.messages.UiMessages

internal fun dateLimitLabel(value: Int): String =
    dateLimitChoices.firstOrNull { it.value.parseIntOrNull() == value }?.label ?: UiMessages.app_anyTime()
