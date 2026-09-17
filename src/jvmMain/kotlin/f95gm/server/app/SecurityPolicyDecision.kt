package f95gm.server.app

internal sealed interface SecurityPolicyDecision {
    data object Allowed : SecurityPolicyDecision
    data class Rejected(val reason: SecurityRejectionReason) : SecurityPolicyDecision
}

internal enum class SecurityRejectionReason { ORIGIN, RATE_LIMIT, AUTHENTICATION }
