package f95gm.client.data.api

import f95gm.domain.api.ApiPath
import f95gm.domain.api.ApiResponseEnvelope
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import f95gm.client.state.runtime.ClientClock
import f95gm.client.state.runtime.SystemClientClock
import f95gm.client.model.runtime.HttpStatusCodeValue
import kotlin.time.Duration

/** Single client boundary for transport, status handling, API errors, and response metrics. */
internal class ApiRequestExecutor(
    internal val transport: ApiTransport,
    private val events: ApiClientEvents,
    private val decoder: ApiResponseDecoder = ApiResponseDecoder(),
    private val clock: ClientClock = SystemClientClock,
    private val requestIds: ApiRequestIdGenerator = SequentialApiRequestIdGenerator()
) {
    @Suppress("TooGenericExceptionCaught") // Decoder failures are normalized at this single transport boundary.
    suspend fun execute(context: ApiRequestContext): ApiResponseEnvelope<String> {
        val path = context.path
        val startedAt = clock.monotonic()
        val requestId = requestIds.next()
        val response = apiRequest(path, transport = transport, events = events, request = { apiTransport ->
            apiTransport.request(path, context.retryPolicy) {
                this.method = context.method
                context.contentType?.let { contentType(ContentType.parse(it.value)) }
                context.headers.values.forEach { header(it.name.value, it.value.value) }
                context.body?.let {
                    if (context.contentType == null) contentType(ContentType.Application.Json)
                    setBody(it.value)
                }
            }
        }, requestId = requestId, endpoint = context.endpoint)
        val status = HttpStatusCodeValue(response.status.value)
        return try {
            decoder.decode(path, response, context.fallback).also {
                recordCompletion(path, status, startedAt, requestId, context.endpoint)
            }
        } catch (error: ApiException) {
            error.requestId = requestId
            recordCompletion(path, status, startedAt, requestId, context.endpoint, error)
            rethrow(error)
        } catch (error: CancellationException) {
            rethrow(error)
        } catch (error: RuntimeException) {
            val normalized = normalizeApiException(error, path, ApiExceptionCause.PAYLOAD)
            normalized.requestId = requestId
            recordCompletion(path, status, startedAt, requestId, context.endpoint, normalized)
            rethrow(normalized)
        }
    }

    private fun recordCompletion(
        path: ApiPath,
        status: HttpStatusCodeValue,
        startedAt: Duration,
        requestId: f95gm.client.model.runtime.ApiRequestId,
        endpoint: f95gm.client.model.runtime.ApiEndpointId,
        failure: ApiException? = null
    ) {
        events.emit(ApiTelemetryEvent.Completed(path, status, elapsedMillis(clock, startedAt), requestId, endpoint))
        failure?.let {
            events.emit(ApiTelemetryEvent.Failure(path, it.kind, it.message ?: apiErrorFallback, requestId, endpoint))
        }
    }
}

private fun elapsedMillis(clock: ClientClock, startedAt: Duration): Long =
    (clock.monotonic() - startedAt).coerceAtLeast(Duration.ZERO).inWholeMilliseconds

private fun rethrow(error: Throwable): Nothing = throw error
