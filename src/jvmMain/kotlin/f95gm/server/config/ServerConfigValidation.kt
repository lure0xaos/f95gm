package f95gm.server.config


internal data class ServerConfigValidation(
    val issues: List<ConfigurationValidationIssue>
) {
    val isValid: Boolean get() = issues.isEmpty()

    fun throwIfInvalid() {
        if (!isValid) throw ConfigurationValidationException(issues)
    }
}

internal fun ServerConfig.validate(): ServerConfigValidation = validateResult().also {
    it.throwIfInvalid()
}

internal fun ServerConfig.validateResult(): ServerConfigValidation {
    val config = this
    return ServerConfigValidation(buildList {
        check("host") { serverHost }
        check("database") { databaseLocation }
        check("port") { port }
        checkIssues("timeouts") { ServerConfigValidators.timeouts(timeouts) }
        checkIssues("limits") { ServerConfigValidators.limits(limits, timeouts) }
        checkIssues("traffic") { ServerConfigValidators.traffic(traffic) }
        checkIssues("upstreamRetry") { ServerConfigValidators.upstreamRetry(upstreamRetry) }
        checkIssues("databaseRetry") { ServerConfigValidators.databaseRetry(databaseRetry) }
        checkIssues("cross-field", ConfigurationValidationCode.CROSS_FIELD) { ServerConfigValidators.crossField(config) }
        checkIssues("deploymentMode", ConfigurationValidationCode.UNSUPPORTED_VALUE) {
            ServerConfigValidators.deploymentMode(deploymentMode)
        }
        check("staticDirectory") { staticDirectoryPath }
    })
}

private inline fun MutableList<ConfigurationValidationIssue>.check(
    name: String,
    code: ConfigurationValidationCode = ConfigurationValidationCode.INVALID_VALUE,
    block: () -> Unit
) {
    try {
        block()
    } catch (error: IllegalArgumentException) {
        add(ConfigurationValidationIssue(name, error.message ?: error::class.simpleName.orEmpty(), code))
    } catch (error: IllegalStateException) {
        add(ConfigurationValidationIssue(name, error.message ?: error::class.simpleName.orEmpty(), code))
    }
}

private inline fun MutableList<ConfigurationValidationIssue>.checkIssues(
    name: String,
    code: ConfigurationValidationCode = ConfigurationValidationCode.INVALID_VALUE,
    issues: () -> List<ConfigurationIssue>
) {
    issues().forEach { issue ->
        val issueCode = if (issue.code == ConfigurationValidationCode.INVALID_VALUE) code else issue.code
        add(ConfigurationValidationIssue(name, issue.message, issueCode))
    }
}

internal fun ServerConfig.redactedSummary() = RedactedServerConfig(
    host = host,
    port = port.value,
    database = if (database.isBlank()) "<default>" else "<configured>",
    staticDirectoryConfigured = staticDirectory != null,
    deploymentMode = deploymentMode.name,
    openBrowser = openBrowser
)
