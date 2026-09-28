package f95gm.server.config

internal interface ConfigurationSource {
    val name: String get() = this::class.simpleName ?: "configuration-source"
    fun properties(): Map<String, String>
    fun environment(): Map<String, String>
    fun inputs(): ConfigurationInputs = ConfigurationInputs.from(properties(), environment())
}

internal enum class ConfigurationValueSource { DEFAULT, SYSTEM_PROPERTY, ENVIRONMENT }

@JvmInline
internal value class ConfigurationPropertyName(val value: String)

@JvmInline
internal value class ConfigurationEnvironmentName(val value: String)

internal data class ResolvedConfigurationValue(
    val value: String,
    val source: ConfigurationValueSource
)

@JvmInline
internal value class ConfigurationPropertyValues private constructor(val values: Map<String, String>) {
    companion object {
        fun copyOf(values: Map<String, String>): ConfigurationPropertyValues =
            ConfigurationPropertyValues(values.toMap())
    }
}

@JvmInline
internal value class ConfigurationEnvironmentValues private constructor(val values: Map<String, String>) {
    companion object {
        fun copyOf(values: Map<String, String>): ConfigurationEnvironmentValues =
            ConfigurationEnvironmentValues(values.toMap())
    }
}

internal data class ConfigurationInputs(
    val properties: ConfigurationPropertyValues,
    val environment: ConfigurationEnvironmentValues
) {
    companion object {
        fun from(properties: Map<String, String>, environment: Map<String, String>): ConfigurationInputs =
            ConfigurationInputs(
                ConfigurationPropertyValues.copyOf(properties),
                ConfigurationEnvironmentValues.copyOf(environment)
            )
    }

    fun resolveValue(key: ConfigKey<*>): ResolvedConfigurationValue {
        val name = key.propertyName
        val environmentName = key.environmentName
        val source = when {
            properties.values.containsKey(name.value) -> ConfigurationValueSource.SYSTEM_PROPERTY
            environment.values.containsKey(environmentName.value) -> ConfigurationValueSource.ENVIRONMENT
            else -> ConfigurationValueSource.DEFAULT
        }
        return ResolvedConfigurationValue(
            when (source) {
                ConfigurationValueSource.SYSTEM_PROPERTY -> properties.values.getValue(name.value)
                ConfigurationValueSource.ENVIRONMENT -> environment.values.getValue(environmentName.value)
                ConfigurationValueSource.DEFAULT -> key.encodedDefault()
            },
            source
        )
    }

    fun source(key: ConfigKey<*>): ConfigurationValueSource = when {
        properties.values.containsKey(key.propertyName.value) -> ConfigurationValueSource.SYSTEM_PROPERTY
        environment.values.containsKey(key.environmentName.value) -> ConfigurationValueSource.ENVIRONMENT
        else -> ConfigurationValueSource.DEFAULT
    }
}

internal fun configurationEnvironmentName(propertyName: ConfigPropertyName): ConfigurationEnvironmentName =
    ConfigurationEnvironmentName(
        "F95GM_" + propertyName.value
            .removePrefix("f95gm.")
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .uppercase()
    )
