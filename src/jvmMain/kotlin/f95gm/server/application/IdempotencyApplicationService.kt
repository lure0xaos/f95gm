package f95gm.server.application

import f95gm.server.application.port.IdempotencyApplicationPort
import f95gm.server.application.port.IdempotencyDecision
import f95gm.server.application.port.IdempotencyRejectionReason
import f95gm.server.application.port.IdempotencyKey
import f95gm.server.application.port.IdempotencyFingerprint
import f95gm.server.application.port.CachedResponse
import f95gm.server.application.port.IdempotencyLookup
import f95gm.server.application.port.IdempotencyClaimResult

/** Application boundary for idempotent mutations; it has no HTTP dependencies. */
internal class IdempotencyApplicationService(private val registry: IdempotencyRegistry) : IdempotencyApplicationPort {
    override fun begin(key: IdempotencyKey?, fingerprint: IdempotencyFingerprint): IdempotencyDecision {
        if (key == null) return IdempotencyDecision.NotRequired
        when (val lookup = registry.lookupResult(key, fingerprint)) {
            is IdempotencyLookup.Completed -> return IdempotencyDecision.Replay(lookup.response)
            IdempotencyLookup.FingerprintMismatch -> return IdempotencyDecision.Rejected(IdempotencyRejectionReason.FINGERPRINT_MISMATCH)
            IdempotencyLookup.InProgress -> return IdempotencyDecision.Rejected(IdempotencyRejectionReason.IN_PROGRESS)
            IdempotencyLookup.Missing, IdempotencyLookup.Expired, IdempotencyLookup.Failed -> Unit
        }
        return when (registry.claimResult(key, fingerprint)) {
            IdempotencyClaimResult.Claimed -> IdempotencyDecision.Claimed(key, fingerprint)
            IdempotencyClaimResult.FingerprintMismatch -> IdempotencyDecision.Rejected(IdempotencyRejectionReason.FINGERPRINT_MISMATCH)
            IdempotencyClaimResult.InProgress -> IdempotencyDecision.Rejected(IdempotencyRejectionReason.IN_PROGRESS)
        }
    }

    fun lookup(key: IdempotencyKey, fingerprint: IdempotencyFingerprint) = registry.lookup(key, fingerprint)
    fun claim(key: IdempotencyKey, fingerprint: IdempotencyFingerprint) = registry.claim(key, fingerprint)
    fun hasFingerprintMismatch(key: IdempotencyKey, fingerprint: IdempotencyFingerprint) =
        registry.hasFingerprintMismatch(key, fingerprint)
    override fun complete(key: IdempotencyKey, fingerprint: IdempotencyFingerprint, response: CachedResponse) =
        registry.complete(key, fingerprint, response)
    override fun release(key: IdempotencyKey) = registry.release(key)
}
