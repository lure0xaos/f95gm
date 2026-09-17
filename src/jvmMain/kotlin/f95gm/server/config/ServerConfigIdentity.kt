package f95gm.server.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class RedactedServerConfig(
    val host: String,
    val port: Int,
    val database: String,
    val staticDirectoryConfigured: Boolean,
    val deploymentMode: String,
    val openBrowser: Boolean
)

internal enum class DeploymentMode { SINGLE_INSTANCE }

internal enum class ConfigurationErrorCode { INVALID_VALUE }

internal class ServerConfigurationException(
    property: String,
    supplied: String,
    expected: String,
    val code: ConfigurationErrorCode = ConfigurationErrorCode.INVALID_VALUE,
    val issues: List<ConfigurationInputIssue> = emptyList()
) : IllegalStateException("Invalid $property='${ConfigurationSuppliedValue(property, supplied).display}'; expected $expected")

@Serializable
internal enum class ConfigurationValidationCode { INVALID_VALUE, CROSS_FIELD, UNSUPPORTED_VALUE }

internal data class ConfigurationIssue(
    val message: String,
    val code: ConfigurationValidationCode = ConfigurationValidationCode.INVALID_VALUE
)

@Serializable
internal data class ConfigurationValidationIssue(
    val field: String,
    val message: String,
    val code: ConfigurationValidationCode = ConfigurationValidationCode.INVALID_VALUE
) {
    override fun toString(): String = "$field: $message"
}

internal class ConfigurationValidationException(val errors: List<ConfigurationValidationIssue>) : IllegalArgumentException(
    errors.joinToString(prefix = "Invalid server configuration: ", separator = "; ")
) {
    /** Stable machine-readable diagnostics for startup tooling and support bundles. */
    fun diagnosticsJson(): String = Json.encodeToString(errors)
}

internal class ConfigurationReloadRejected(val reasons: List<RestartRequirement>) : IllegalArgumentException(
    "Configuration requires restart: ${reasons.joinToString { it.wireName }}."
)
