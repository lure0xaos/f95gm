package f95gm.client.model.runtime

internal enum class UpstreamConnectionStatus {
    UNKNOWN,
    RESPONDING,
    AUTHENTICATION_REQUIRED,
    UNAVAILABLE,
    ERROR
}
