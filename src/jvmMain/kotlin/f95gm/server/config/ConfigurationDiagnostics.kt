package f95gm.server.config

/** Collects provenance and validation issues without coupling them to property parsing. */
internal data class ConfigurationDiagnostics(val sources: Map<ConfigPropertyName, ConfigurationValueSource>) {
    operator fun get(key: ConfigKey<*>): ConfigurationValueSource? = sources[key.propertyName]
}

internal class ConfigurationDiagnosticsCollector {
    private var errors: Map<String, ConfigurationInputIssue> = emptyMap()
    private var sources: Map<ConfigPropertyName, ConfigurationValueSource> = emptyMap()

    fun recordSource(name: ConfigPropertyName, source: ConfigurationValueSource) {
        sources = sources + (name to source)
    }

    fun recordIssue(property: String, issue: ConfigurationInputIssue) {
        if (property !in errors) errors = errors + (property to issue)
    }

    fun provenance(): Map<ConfigPropertyName, ConfigurationValueSource> = sources

    fun snapshot(): ConfigProperties.Snapshot = ConfigProperties.Snapshot(sources, errors.values.toList())
}

internal data class ConfigurationInputIssue(
    val property: String,
    val supplied: String,
    val expected: String,
    val sensitive: Boolean = false
) {
    val propertyName: ConfigPropertyName get() = ConfigPropertyName(property)
    val suppliedValue: ConfigurationSuppliedValue get() = ConfigurationSuppliedValue(property, supplied, sensitive)
    override fun toString(): String = "$property='${suppliedValue.display}'; expected $expected"
}

internal data class ConfigurationSuppliedValue(
    private val property: String,
    internal val raw: String,
    private val sensitive: Boolean = false
) {
    val display: String
        get() = if (sensitive || property.lowercase().containsAny("password", "secret", "token", "cookie", "authorization")) {
            "<redacted>"
        } else raw

    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)
}
