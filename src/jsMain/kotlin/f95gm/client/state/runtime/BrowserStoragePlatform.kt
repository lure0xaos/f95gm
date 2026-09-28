package f95gm.client.state.runtime

/**
 * Maps browser-specific storage exceptions to the platform-neutral failure type used by the client.
 * This is the only place where Kotlin/JS dynamic exception metadata is inspected.
 */
internal fun Throwable.browserStorageFailure(): BrowserStorageFailure? = when (asDynamic().name) {
    "SecurityError" -> BrowserStorageFailure.SECURITY_RESTRICTION
    "QuotaExceededError" -> BrowserStorageFailure.QUOTA_EXCEEDED
    else -> null
}
