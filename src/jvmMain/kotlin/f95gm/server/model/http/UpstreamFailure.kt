package f95gm.server.model.http

internal enum class UpstreamFailure(
    val userMessage: String,
    val retryable: Boolean,
    val tripsCircuit: Boolean
) {
    CIRCUIT_OPEN("The upstream service is temporarily unavailable", retryable = false, tripsCircuit = false),
    RESPONSE_TOO_LARGE("The upstream response was too large", retryable = false, tripsCircuit = false),
    CONNECTION_FAILURE("The upstream service could not be reached", retryable = true, tripsCircuit = true),
    TIMEOUT("The upstream service timed out", retryable = true, tripsCircuit = true),
    MALFORMED_RESPONSE("The upstream service returned an invalid response", retryable = false, tripsCircuit = true)
}
