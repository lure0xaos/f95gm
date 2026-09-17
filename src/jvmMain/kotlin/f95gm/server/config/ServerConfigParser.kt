package f95gm.server.config

/** Converts external properties and environment variables into a validated config shape. */
internal object ServerConfigParser {
    fun parse(values: Map<String, String>, environment: Map<String, String>): ServerConfig {
        val inputs = ConfigurationInputs.from(values, environment)
        val properties = ConfigProperties(
            getProperty = { key -> inputs.resolveValue(key).value },
            getSource = { key -> inputs.resolveValue(key).source }
        )
        val config = ServerConfig.create(
            host = properties.text(Keys.HOST),
            port = ServerPort(properties.port(Keys.PORT)),
            database = properties.text(Keys.DATABASE).trim(),
            staticDirectory = properties.text(Keys.STATIC_DIRECTORY).trim().takeIf { it.isNotEmpty() },
            timeouts = TimeoutConfig.from(properties),
            limits = ResourceLimits.from(properties),
            traffic = TrafficConfig.from(properties),
            upstreamRetry = UpstreamRetryConfig.from(properties),
            databaseRetryMaxTotalDelayMillis = properties.positiveDuration(Keys.DATABASE_RETRY_MAX_TOTAL_DELAY),
            deploymentMode = properties.deploymentMode(Keys.DEPLOYMENT_MODE),
            openBrowser = properties.boolean(Keys.OPEN_BROWSER),
            upstreamBaseUrl = ConfigurationValidation.upstreamOrigin(
                normalizeUpstreamBaseUrl(properties.text(Keys.UPSTREAM_BASE_URL))
            )
        )
        val snapshot = properties.snapshot()
        if (snapshot.issues.isNotEmpty()) {
            throw ServerConfigurationException(
                "configuration",
                snapshot.issues.joinToString("; "),
                "valid configured values",
                issues = snapshot.issues
            )
        }
        return config.copy(configurationDiagnostics = ConfigurationDiagnostics(snapshot.provenance))
    }

    private fun normalizeUpstreamBaseUrl(value: String): String =
        value.trim().trimEnd('/').ifBlank { "https://f95zone.to" }

}
