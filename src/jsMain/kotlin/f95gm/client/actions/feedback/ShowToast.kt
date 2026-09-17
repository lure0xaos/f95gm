package f95gm.client.actions.feedback

import f95gm.client.model.feedback.ToastState
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.runtime.ClientFeedbackActionsPort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun showToast(runtime: ClientFeedbackActionsPort, message: String) {
    runtime.launchEffect {
        runtime.uiEffects.toast(message)
        runtime.clock.delay(ClientConstants.TOAST_DURATION_MILLIS.milliseconds)
        if (runtime.toast.data.first().message == message) runtime.toast.enqueue { ToastState() }
    }
}
