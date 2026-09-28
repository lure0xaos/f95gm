package f95gm.server.config

import f95gm.domain.api.parsePositiveLong
import f95gm.domain.api.parseIntOrNull
import f95gm.server.kernel.ResourceLock


internal class ConfigProperties(
    private val getProperty: (ConfigKey<*>) -> String,
    private val getSource: (ConfigKey<*>) -> ConfigurationValueSource
) {
    internal constructor(getProperty: (ConfigKey<*>, String) -> String) : this(
        { key -> getProperty(key, key.encodedDefault()) },
        { ConfigurationValueSource.DEFAULT }
    )

    private val diagnostics = ConfigurationDiagnosticsCollector()
    private val lock = ResourceLock()
    private var frozen = false
    private var frozenSnapshot: Snapshot? = null

    internal data class Snapshot(
        val provenance: Map<ConfigPropertyName, ConfigurationValueSource>,
        val issues: List<ConfigurationInputIssue>
    )

    private fun raw(key: ConfigKey<*>): String {
        check(!frozen) { "Configuration properties cannot be read after validation" }
        return getProperty(key).also {
            diagnostics.recordSource(key.propertyName, getSource(key))
        }
    }


    fun provenance(): Map<ConfigPropertyName, ConfigurationValueSource> = lock.withLock { diagnostics.provenance() }

    fun snapshot(): Snapshot = lock.withLock {
        frozenSnapshot?.let { return@withLock it }
        frozen = true
        diagnostics.snapshot().also { frozenSnapshot = it }
    }

    fun text(key: TextKey): String = lock.withLock { read(key, "text") { it } }

    fun deploymentMode(key: TextKey): DeploymentMode = lock.withLock {
        val supplied = raw(key)
        parse(key.name, supplied, DeploymentMode.entries.joinToString(), DeploymentMode.SINGLE_INSTANCE) {
            DeploymentMode.entries.firstOrNull { it.name == supplied.trim().uppercase() }
        }
    }

    fun port(key: IntKey): Int = lock.withLock {
        read(
            key,
            "an integer between ${ServerDefaults.MIN_PORT} and ${ServerDefaults.MAX_PORT}",
            normalize = String::trim
        ) { it.parseIntOrNull()?.takeIf { value -> value in ServerDefaults.MIN_PORT..ServerDefaults.MAX_PORT } }
    }

    fun positiveLong(key: LongKey): Long = lock.withLock { read(key, "a positive integer", normalize = String::trim) {
        it.parsePositiveLong()
    } }

    fun positiveDuration(key: DurationKey): Millis = lock.withLock { read(key, "a positive duration in milliseconds", normalize = String::trim) {
        it.parsePositiveLong()?.let(::Millis)
    } }

    fun positiveBytes(key: ByteSizeKey): Bytes = lock.withLock { read(key, "a positive byte size", normalize = String::trim) {
        it.parsePositiveLong()?.let(::Bytes)
    } }

    fun positiveInt(key: IntKey): Int = lock.withLock { read(key, "a positive integer", normalize = String::trim) {
        it.parseIntOrNull()?.takeIf { value -> value > 0 }
    } }

    fun boolean(key: BooleanKey): Boolean = lock.withLock { read(key, "true or false", normalize = { it.trim().lowercase() }) {
        value -> when (value) { "true" -> true; "false" -> false; else -> null }
    } }

    fun throwIfInvalid() = lock.withLock {
        val snapshot = snapshot()
        if (snapshot.issues.isNotEmpty()) {
            throw ServerConfigurationException(
                "configuration",
                snapshot.issues.joinToString("; "),
                "valid configured values",
                issues = snapshot.issues
            )
        }
    }

    private fun <T> parse(
        property: String,
        raw: String,
        expected: String,
        fallback: T,
        parser: (String) -> T?
    ): T = when (val result = parseResult(property, raw, expected, parser)) {
        is ConfigParseResult.Valid -> result.value
        is ConfigParseResult.Invalid -> {
            diagnostics.recordIssue(
                result.property.value,
                ConfigurationInputIssue(
                    result.property.value,
                    result.supplied.raw,
                    result.expected,
                    result.property in Keys.sensitive
                )
            )
            fallback
        }
    }

    private fun <T> read(
        key: ConfigKey<T>,
        expected: String,
        normalize: (String) -> String = { it },
        parser: (String) -> T?
    ): T {
        val supplied = raw(key)
        return parse(key.propertyName.value, supplied, expected, key.default) { parser(normalize(it)) }
    }

    private fun <T> parseResult(
        property: String,
        raw: String,
        expected: String,
        parser: (String) -> T?
    ): ConfigParseResult<T> = parser(raw)?.let(ConfigParseResult<T>::Valid)
        ?: ConfigParseResult.Invalid(ConfigPropertyName(property), ConfigurationSuppliedValue(property, raw), expected)
}
