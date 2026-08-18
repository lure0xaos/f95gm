package f95gm.client.ui.navigation

import f95gm.client.model.runtime.UpstreamConnectionStatus
import f95gm.messages.UiMessages

internal fun upstreamConnectionStatusLabel(status: UpstreamConnectionStatus): String = when (status) {
    UpstreamConnectionStatus.UNKNOWN -> UiMessages.app_notChecked()
    UpstreamConnectionStatus.RESPONDING -> UiMessages.app_responding()
    UpstreamConnectionStatus.AUTHENTICATION_REQUIRED -> UiMessages.app_authenticationRequired()
    UpstreamConnectionStatus.UNAVAILABLE -> UiMessages.app_unavailable()
    UpstreamConnectionStatus.ERROR -> UiMessages.app_unexpectedResponse()
}
