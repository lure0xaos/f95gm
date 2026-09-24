package f95gm.server.http.request

import f95gm.server.state.runtime.IdempotencyRegistry

internal class MutationService(private val registry: IdempotencyRegistry) {
    fun lookup(key: String, fingerprint: String) = registry.lookup(key, fingerprint)
    fun claim(key: String, fingerprint: String) = registry.claim(key, fingerprint)
    fun hasFingerprintMismatch(key: String, fingerprint: String) = registry.hasFingerprintMismatch(key, fingerprint)
    fun complete(key: String, fingerprint: String, response: IdempotencyRegistry.CachedResponse) =
        registry.complete(key, fingerprint, response)

    fun release(key: String) = registry.release(key)
}
