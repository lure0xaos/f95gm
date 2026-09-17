package f95gm.server.cache

import kotlinx.coroutines.CancellationException

/** Captures loader failures without converting coroutine cancellation into a cache failure. */
@Suppress("TooGenericExceptionCaught") // Cache loading is the boundary that converts ordinary loader exceptions to Result.
internal inline fun <T> cacheLoad(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}

/** Suspending counterpart that preserves cancellation while capturing ordinary loader failures. */
@Suppress("TooGenericExceptionCaught") // Cache loading is the boundary that converts ordinary loader exceptions to Result.
internal suspend fun <T> cacheLoadSuspending(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
