package f95gm.client.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val failure: ApiFailure) : ApiResult<Nothing>

    suspend fun onSuccess(action: suspend (T) -> Unit): ApiResult<T> {
        if (this is Success) action(value)
        return this
    }

    suspend fun onFailure(action: suspend (ApiFailure) -> Unit): ApiResult<T> {
        if (this is Failure) action(failure)
        return this
    }
}

internal suspend inline fun <T> apiResult(crossinline block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: ApiException) {
    ApiResult.Failure(error.failure)
} catch (error: SerializationException) {
    ApiResult.Failure(
        ApiException(
            path = f95gm.domain.api.ApiPath(""),
            message = error.message ?: apiErrorFallback,
            causeKind = ApiExceptionCause.PAYLOAD,
            cause = error
        ).failure
    )
} catch (error: IllegalArgumentException) {
    ApiResult.Failure(normalizeApiException(error).failure)
}
