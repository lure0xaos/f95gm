package f95gm.client.data.api

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import f95gm.domain.api.ApiPath
import f95gm.client.state.runtime.ClientClock
import f95gm.client.state.runtime.SystemClientClock
import f95gm.domain.api.parseRetryAfter

internal class ApiTransport(
    private val client: HttpClient,
    private val clock: ClientClock = SystemClientClock
) {
    suspend fun request(
        path: ApiPath,
        retryPolicy: RetryPolicy = RetryPolicy(),
        configure: HttpRequestBuilder.() -> Unit
    ): HttpResponse = withTransportRetry(
        policy = retryPolicy,
        pause = clock::delay,
        retryableResult = { response ->
            if (!retryPolicy.shouldRetryStatus(response.status.value)) {
                null
            } else {
                parseRetryAfter(response.headers["Retry-After"], clock.now().toEpochMilliseconds())?.duration
                    ?: kotlin.time.Duration.ZERO
            }
        }
    ) {
        try {
            client.request(path.value, configure)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw ApiTransportException(path, error)
        } catch (error: HttpRequestTimeoutException) {
            throw ApiTransportException(path, error)
        }
    }
}
