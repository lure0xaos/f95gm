package f95gm.client.state.runtime

/** Common lifecycle representation for asynchronous client-owned data. */
internal sealed interface AsyncState<out T> {
    data object Idle : AsyncState<Nothing>
    data object Loading : AsyncState<Nothing>
    data class Refreshing<T>(val previous: T) : AsyncState<T>
    data class Success<T>(val value: T) : AsyncState<T>
    data class Failure(val message: String) : AsyncState<Nothing>
    data object Cancelled : AsyncState<Nothing>
}

internal val AsyncState<*>.isLoading: Boolean get() = this is AsyncState.Loading || this is AsyncState.Refreshing<*>
internal val AsyncState<*>.errorMessage: String get() = (this as? AsyncState.Failure)?.message.orEmpty()
