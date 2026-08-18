package f95gm.client.actions.feedback

import f95gm.client.model.feedback.ToastState
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.feedback.toast
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun showToast(message: String) {
    scope.launch {
        toast.enqueue { ToastState(message) }
        delay(ClientConstants.TOAST_DURATION_MILLIS.milliseconds)
        if (toast.data.first().message == message) toast.enqueue { ToastState() }
    }
}
