package f95gm.server.kernel

/** One-shot completion/failure signal without lifecycle phase semantics. */
internal class CompletionSignal {
    private val state = CompletionState<Unit>()

    suspend fun await() {
        when (val result = state.await()) {
            is CompletionCell.Outcome.Success -> Unit
            is CompletionCell.Outcome.Failure -> throw result.error
        }
    }

    fun complete() = state.complete(Unit)
    fun fail(error: Throwable) = state.fail(error)
}
