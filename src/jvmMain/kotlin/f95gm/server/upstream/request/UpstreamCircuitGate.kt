package f95gm.server.upstream.request

/** Keeps circuit admission separate from retry execution and response processing. */
internal class UpstreamCircuitGate(
    private val breaker: UpstreamCircuitBreaker
) {
    internal sealed interface Decision {
        data object Allowed : Decision
        data object Open : Decision
    }

    fun evaluate(key: UpstreamCircuitKey): Decision =
        if (breaker.allow(key)) Decision.Allowed else Decision.Open
}
