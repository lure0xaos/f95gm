package f95gm.client.data.api

import f95gm.messages.UiMessages

internal val apiErrorFallback: String
    get() = UiMessages.app_catalogServiceUnavailable()
