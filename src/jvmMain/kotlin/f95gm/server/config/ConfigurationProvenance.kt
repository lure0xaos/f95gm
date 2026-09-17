package f95gm.server.config

internal enum class ConfigurationOrigin {
    PROGRAMMATIC,
    SYSTEM_PROPERTIES
}

internal data class ConfigurationProvenance(
    val origin: ConfigurationOrigin,
    val sourceName: String
) {
    companion object {
        fun programmatic(): ConfigurationProvenance = ConfigurationProvenance(
            ConfigurationOrigin.PROGRAMMATIC,
            "programmatic"
        )

        fun from(source: ConfigurationSource): ConfigurationProvenance = ConfigurationProvenance(
            ConfigurationOrigin.SYSTEM_PROPERTIES,
            source.name
        )
    }
}
