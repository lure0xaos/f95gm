package f95gm.server.kernel

/** Gives independently-owned resources one consistent, idempotent close boundary. */
internal class CloseGuard {
    private val lifecycle = LifecycleGate()

    val isClosed: Boolean get() = lifecycle.isClosed

    fun requestClose(): Boolean = lifecycle.beginClose()

    @Suppress("TooGenericExceptionCaught") // The guard must preserve the first failure for both ordinary and fatal close failures.
    fun closeOnce(block: () -> Unit) {
        if (!requestClose()) return
        try {
            block()
            lifecycle.complete()
        } catch (failure: Throwable) {
            lifecycle.fail(failure)
            throw failure
        }
    }

    @Suppress("TooGenericExceptionCaught") // The guard must preserve the first failure for both ordinary and fatal close failures.
    suspend fun closeOnceSuspending(block: suspend () -> Unit) {
        if (!requestClose()) return
        try {
            block()
            lifecycle.complete()
        } catch (failure: Throwable) {
            lifecycle.fail(failure)
            throw failure
        }
    }
}
