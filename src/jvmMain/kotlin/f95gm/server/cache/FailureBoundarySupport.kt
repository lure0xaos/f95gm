package f95gm.server.cache

internal inline fun rethrowAfterFailure(
    error: Throwable,
    onFailure: (Throwable) -> Unit
): Nothing {
    onFailure(error)
    throw error
}
