package f95gm.client.state.feedback

import dev.fritz2.core.RootStore
import f95gm.client.model.feedback.ToastState
import kotlinx.coroutines.Job

internal val toast = RootStore(ToastState(), Job())
