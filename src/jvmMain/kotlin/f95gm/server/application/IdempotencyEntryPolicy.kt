package f95gm.server.application

import f95gm.server.application.port.IdempotencyFingerprint

internal object IdempotencyEntryPolicy {
    fun canRetry(entry: IdempotencyRegistry.EntryState, fingerprint: IdempotencyFingerprint): Boolean =
        entry is IdempotencyRegistry.EntryState.Failed && entry.fingerprint == fingerprint

    fun retainAfterRelease(expiresAt: Long, now: Long): Boolean = expiresAt > now
}
