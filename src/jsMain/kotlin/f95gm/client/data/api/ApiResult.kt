package f95gm.client.data.api

import kotlinx.coroutines.CancellationException

internal sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiException) : ApiResult<Nothing>

    suspend fun onSuccess(action: suspend (T) -> Unit): ApiResult<T> {
        if (this is Success) action(value)
        return this
    }

    suspend fun onFailure(action: suspend (ApiException) -> Unit): ApiResult<T> {
        if (this is Failure) action(error)
        return this
    }
}

internal suspend inline fun <T> apiResult(crossinline block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    ApiResult.Failure(normalizeApiException(error))
}
