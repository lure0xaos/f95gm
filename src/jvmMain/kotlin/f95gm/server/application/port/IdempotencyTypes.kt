package f95gm.server.application.port

import f95gm.domain.api.ApiStatusCode

@JvmInline
internal value class IdempotencyKey(val value: String) {
    init { require(value.isNotBlank() && value.length <= 256) }
}

@JvmInline
internal value class IdempotencyFingerprint(val value: String)

internal data class CachedResponse(
    val status: ApiStatusCode,
    val body: String,
    val contentType: String? = null
)

internal sealed interface IdempotencyLookup {
    data object Missing : IdempotencyLookup
    data object Expired : IdempotencyLookup
    data object FingerprintMismatch : IdempotencyLookup
    data object InProgress : IdempotencyLookup
    data object Failed : IdempotencyLookup
    data class Completed(val response: CachedResponse) : IdempotencyLookup
}

internal sealed interface IdempotencyClaimResult {
    data object Claimed : IdempotencyClaimResult
    data object InProgress : IdempotencyClaimResult
    data object FingerprintMismatch : IdempotencyClaimResult
}
