package f95gm.server.application.port

internal interface IdempotencyApplicationPort {
    fun begin(key: IdempotencyKey?, fingerprint: IdempotencyFingerprint): IdempotencyDecision
    fun complete(key: IdempotencyKey, fingerprint: IdempotencyFingerprint, response: CachedResponse)
    fun release(key: IdempotencyKey)
}

internal enum class IdempotencyRejectionReason { FINGERPRINT_MISMATCH, IN_PROGRESS }

internal sealed interface IdempotencyDecision {
    data object NotRequired : IdempotencyDecision
    data class Replay(val response: CachedResponse) : IdempotencyDecision
    data class Rejected(val reason: IdempotencyRejectionReason) : IdempotencyDecision
    data class Claimed(val key: IdempotencyKey, val fingerprint: IdempotencyFingerprint) : IdempotencyDecision
}
