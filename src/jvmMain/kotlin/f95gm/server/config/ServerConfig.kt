package f95gm.server.config

import f95gm.server.config.upstream.UpstreamEndpointConfig

internal data class ServerConfig internal constructor(
    val serverHost: ServerHost,
    val port: ServerPort,
    val databaseLocation: DatabaseLocation,
    val staticDirectoryPath: StaticDirectoryPath?,
    val timeouts: TimeoutConfig = TimeoutConfig(),
    val limits: ResourceLimits = ResourceLimits(),
    val traffic: TrafficConfig = TrafficConfig(),
    val upstreamRetry: UpstreamRetryConfig = UpstreamRetryConfig(),
    val databaseRetry: DatabaseRetryConfig = DatabaseRetryConfig(),
    val deploymentMode: DeploymentMode = DeploymentMode.SINGLE_INSTANCE,
    val openBrowser: Boolean = true,
    val upstreamBaseUrl: String = "https://f95zone.to",
    val configurationDiagnostics: ConfigurationDiagnostics = ConfigurationDiagnostics(emptyMap())
) {
    val configurationSources: Map<ConfigPropertyName, ConfigurationValueSource>
        get() = configurationDiagnostics.sources
    val upstreamEndpoints: UpstreamEndpointConfig get() = UpstreamEndpointConfig.from(upstreamBaseUrl)
    val host: String get() = serverHost.value
    val database: String get() = databaseLocation.value
    val staticDirectory: String? get() = staticDirectoryPath?.value

    companion object {
        internal fun create(
            host: String,
            port: ServerPort,
            database: String,
            staticDirectory: String?,
            timeouts: TimeoutConfig = TimeoutConfig(),
            limits: ResourceLimits = ResourceLimits(),
            traffic: TrafficConfig = TrafficConfig(),
            upstreamRetry: UpstreamRetryConfig = UpstreamRetryConfig(),
            databaseRetryMaxTotalDelayMillis: Millis = Millis(RetryLimits.DATABASE_MAX_TOTAL_DELAY_MILLIS),
            deploymentMode: DeploymentMode = DeploymentMode.SINGLE_INSTANCE,
            openBrowser: Boolean = true,
            upstreamBaseUrl: String = "https://f95zone.to",
            configurationDiagnostics: ConfigurationDiagnostics = ConfigurationDiagnostics(emptyMap())
        ): ServerConfig = ServerConfig(
            ServerHost(host), port, DatabaseLocation(database), staticDirectory?.let(::StaticDirectoryPath), timeouts, limits, traffic,
            upstreamRetry, DatabaseRetryConfig(databaseRetryMaxTotalDelayMillis),
            deploymentMode, openBrowser, upstreamBaseUrl, configurationDiagnostics
        )

        fun fromSystemProperties(source: ConfigurationSource): ServerConfig =
            source.inputs().let { fromSources(it.properties.values, it.environment.values) }

        internal fun from(values: Map<String, String>): ServerConfig = fromSources(values, emptyMap())

        internal fun fromSources(values: Map<String, String>, environment: Map<String, String>): ServerConfig =
            ServerConfigParser.parse(values, environment)
    }

}
