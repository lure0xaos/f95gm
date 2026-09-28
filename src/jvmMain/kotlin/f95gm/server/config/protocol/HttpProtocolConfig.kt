package f95gm.server.config.protocol

internal object HttpProtocolConfig {
    const val REQUEST_ID_HEADER = "X-Request-ID"
    const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
}
