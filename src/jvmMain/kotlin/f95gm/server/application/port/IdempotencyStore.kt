package f95gm.server.application.port

internal interface IdempotencyStore {
    fun claim(key: IdempotencyKey, fingerprint: IdempotencyFingerprint = IdempotencyFingerprint("")): Boolean
    fun claimResult(key: IdempotencyKey, fingerprint: IdempotencyFingerprint = IdempotencyFingerprint("")): IdempotencyClaimResult
    fun lookup(key: IdempotencyKey, fingerprint: IdempotencyFingerprint = IdempotencyFingerprint("")): CachedResponse?
    fun hasFingerprintMismatch(key: IdempotencyKey, fingerprint: IdempotencyFingerprint): Boolean
    fun complete(key: IdempotencyKey, fingerprint: IdempotencyFingerprint = IdempotencyFingerprint(""), response: CachedResponse)
    fun release(key: IdempotencyKey)
}
