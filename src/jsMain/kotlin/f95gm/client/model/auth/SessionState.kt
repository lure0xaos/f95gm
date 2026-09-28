package f95gm.client.model.auth

import f95gm.client.state.runtime.AsyncState
import f95gm.client.state.runtime.isLoading
import f95gm.client.state.runtime.errorMessage

internal data class SessionState(
    val authenticated: Boolean = false,
    val async: AsyncState<Unit> = AsyncState.Idle
)

internal val SessionState.busy: Boolean get() = async.isLoading
internal val SessionState.checking: Boolean get() = async.isLoading
internal val SessionState.error: String get() = async.errorMessage
