package f95gm.client.data.api

import f95gm.client.state.runtime.clientJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import f95gm.client.model.runtime.EndpointId

internal data class PayloadPolicy(val maxChars: Int)

internal object PayloadPolicies {
    val Default = PayloadPolicy(1_048_576)
    val Catalog = PayloadPolicy(4 * 1_048_576)
    val Detail = PayloadPolicy(8 * 1_048_576)
}

/** Shared typed boundary for decoding and normalizing client API payloads. */
internal inline fun <reified T, R> parsePayload(
    payload: String,
    json: Json = clientJson,
    policy: PayloadPolicy = PayloadPolicies.Default,
    transform: (T) -> R
): PayloadParseResult<R> {
    if (payload.length > policy.maxChars) return PayloadParseResult.Failure(ParseFailureReason.OVERSIZED_PAYLOAD)
    val decoded = try {
        json.decodeFromString<T>(payload)
    } catch (_: SerializationException) {
        return PayloadParseResult.Failure(ParseFailureReason.INVALID_PAYLOAD)
    } catch (_: IllegalArgumentException) {
        return PayloadParseResult.Failure(ParseFailureReason.INVALID_PAYLOAD)
    } catch (_: IllegalStateException) {
        return PayloadParseResult.Failure(ParseFailureReason.INVALID_PAYLOAD)
    }
    return try {
        PayloadParseResult.Success(transform(decoded))
    } catch (_: PayloadEnvelopeException) {
        PayloadParseResult.Failure(ParseFailureReason.INVALID_ENVELOPE)
    } catch (failure: PayloadMappingException) {
        PayloadParseResult.Failure(failure.reason)
    }
}

internal inline fun <reified T, R> parsePayloadOrThrow(
    payload: String,
    json: Json = clientJson,
    endpoint: EndpointId? = null,
    policy: PayloadPolicy = PayloadPolicies.Default,
    transform: (T) -> R
): R = when (val result = parsePayload(payload, json, policy, transform)) {
    is PayloadParseResult.Success -> result.value
    is PayloadParseResult.Failure -> throw PayloadParseException(result.reason, "Payload is invalid", endpoint = endpoint)
}
