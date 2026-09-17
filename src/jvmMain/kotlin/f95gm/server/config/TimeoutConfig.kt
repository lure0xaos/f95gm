package f95gm.server.config

internal data class TimeoutConfig(
    val requestMillis: Millis = Millis(30_000L),
    val connectMillis: Millis = Millis(10_000L),
    val socketMillis: Millis = Millis(30_000L),
    val shutdownMillis: Millis = Millis(10_000L),
    val sessionIdleMillis: Millis = Millis(ServerDefaults.SESSION_IDLE_MILLIS)
) {
    companion object {
        fun from(properties: ConfigProperties) = TimeoutConfig(
            requestMillis = properties.positiveDuration(Keys.REQUEST_TIMEOUT),
            connectMillis = properties.positiveDuration(Keys.CONNECT_TIMEOUT),
            socketMillis = properties.positiveDuration(Keys.SOCKET_TIMEOUT),
            shutdownMillis = properties.positiveDuration(Keys.SHUTDOWN_TIMEOUT),
            sessionIdleMillis = properties.positiveDuration(Keys.SESSION_IDLE)
        )
    }

    fun validationIssues(): List<ConfigurationIssue> = configurationIssues {
        positive(requestMillis.value, "Request timeout must be positive")
        positive(connectMillis.value, "Connect timeout must be positive")
        positive(socketMillis.value, "Socket timeout must be positive")
        positive(shutdownMillis.value, "Shutdown timeout must be positive")
        atMost(connectMillis.value, requestMillis.value, "Connect timeout must not exceed request timeout")
        atMost(socketMillis.value, requestMillis.value, "Socket timeout must not exceed request timeout")
        atLeast(sessionIdleMillis.value, requestMillis.value, "Session idle timeout must not be shorter than request timeout")
    }

    fun validate() = validationIssues().throwValidation("Timeout configuration")
}
