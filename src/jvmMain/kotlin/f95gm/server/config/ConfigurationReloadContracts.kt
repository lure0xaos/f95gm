package f95gm.server.config

internal enum class ReloadImpact { DYNAMIC, RESTART_REQUIRED }
internal sealed interface ConfigurationReloadOutcome {
    data object Applied : ConfigurationReloadOutcome
    data object Unchanged : ConfigurationReloadOutcome
}

internal enum class ConfigurationPublicationStatus { COMPLETE, LISTENER_FAILURES, EVENT_SINK_FAILURES, LISTENER_AND_EVENT_SINK_FAILURES }

internal sealed interface ConfigurationReloadDecision {
    data object Apply : ConfigurationReloadDecision
    data class RequiresRestart(val reasons: List<RestartRequirement>) : ConfigurationReloadDecision
}

internal enum class RestartRequirement(val wireName: String) {
    LISTENER("listener"), DATABASE("database"), STATIC_DIRECTORY("static directory"),
    DEPLOYMENT_MODE("deployment mode"), UPSTREAM_ENDPOINT("upstream endpoint"),
    HTTP_CLIENT_TIMEOUTS("HTTP client timeouts"), UPSTREAM_RETRY_POLICY("upstream retry policy"),
    DATABASE_RETRY_POLICY("database retry policy"),
    CIRCUIT_BREAKER_POLICY("circuit-breaker policy"), RATE_LIMIT_BUCKET_CAPACITY("rate-limit bucket capacity")
}

internal enum class ConfigurationChangeField(val restartRequirement: RestartRequirement? = null) {
    LISTENER(RestartRequirement.LISTENER),
    DATABASE(RestartRequirement.DATABASE),
    STATIC_DIRECTORY(RestartRequirement.STATIC_DIRECTORY),
    DEPLOYMENT_MODE(RestartRequirement.DEPLOYMENT_MODE),
    UPSTREAM_ENDPOINT(RestartRequirement.UPSTREAM_ENDPOINT),
    HTTP_CLIENT_TIMEOUTS(RestartRequirement.HTTP_CLIENT_TIMEOUTS),
    UPSTREAM_RETRY_POLICY(RestartRequirement.UPSTREAM_RETRY_POLICY),
    DATABASE_RETRY_POLICY(RestartRequirement.DATABASE_RETRY_POLICY),
    CIRCUIT_BREAKER_POLICY(RestartRequirement.CIRCUIT_BREAKER_POLICY),
    RATE_LIMIT_BUCKET_CAPACITY(RestartRequirement.RATE_LIMIT_BUCKET_CAPACITY),
    RESOURCE_LIMITS, IP_RATE_LIMIT, ACCOUNT_RATE_LIMIT, RATE_LIMIT_WINDOW, OPEN_BROWSER, DIAGNOSTICS
}

internal data class ConfigurationChangeSet(val fields: Set<ConfigurationChangeField>) {
    val isEmpty: Boolean get() = fields.isEmpty()
}

internal data class ConfigurationReloadResult(
    val previous: ServerConfigSnapshot,
    val current: ServerConfigSnapshot,
    val impact: ReloadImpact,
    val outcome: ConfigurationReloadOutcome,
    val changes: ConfigurationChangeSet,
    val restartReasons: List<RestartRequirement> = emptyList(),
    val listenerFailures: Int = 0,
    val eventSinkFailures: Int = 0
) {
    val changed: Boolean get() = !changes.isEmpty
    val provenance: ConfigurationProvenance get() = current.provenance
    val publicationStatus: ConfigurationPublicationStatus
        get() = when {
            listenerFailures == 0 && eventSinkFailures == 0 -> ConfigurationPublicationStatus.COMPLETE
            listenerFailures > 0 && eventSinkFailures > 0 -> ConfigurationPublicationStatus.LISTENER_AND_EVENT_SINK_FAILURES
            listenerFailures > 0 -> ConfigurationPublicationStatus.LISTENER_FAILURES
            else -> ConfigurationPublicationStatus.EVENT_SINK_FAILURES
        }
}
