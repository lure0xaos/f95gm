package f95gm.client.ui.navigation

import f95gm.client.model.runtime.JvmConnectionStatus
import f95gm.messages.UiMessages

internal fun jvmConnectionStatusLabel(status: JvmConnectionStatus): String = when (status) {
    JvmConnectionStatus.CONNECTED -> UiMessages.app_connected()
    JvmConnectionStatus.UNAVAILABLE -> UiMessages.app_unavailable()
    JvmConnectionStatus.UNEXPECTED_RESPONSE -> UiMessages.app_unexpectedResponse()
}
