package f95gm.client.data.api

import f95gm.client.model.runtime.EndpointId
import f95gm.client.model.runtime.PayloadField

internal sealed interface PayloadParseResult<out T> {
    data class Success<T>(val value: T) : PayloadParseResult<T>
    data class Failure(val reason: ParseFailureReason) : PayloadParseResult<Nothing>
}

internal enum class ParseFailureReason { INVALID_JSON, INVALID_ENVELOPE, INVALID_PAYLOAD, OVERSIZED_PAYLOAD }

internal class PayloadParseException(
    val reason: ParseFailureReason,
    message: String,
    val endpoint: EndpointId? = null,
    val field: PayloadField? = null
) : RuntimeException(message)

internal class PayloadEnvelopeException(message: String) : RuntimeException(message)

internal class PayloadMappingException(val reason: ParseFailureReason, cause: Throwable? = null) :
    RuntimeException("Payload mapping failed", cause)
