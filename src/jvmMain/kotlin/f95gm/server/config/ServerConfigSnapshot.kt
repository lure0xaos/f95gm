package f95gm.server.config

internal data class ServerConfigSnapshot(
    val value: ServerConfig,
    val provenance: ConfigurationProvenance = ConfigurationProvenance.programmatic(),
    val revision: Long = 0
) {
    companion object {
        fun from(config: ServerConfig): ServerConfigSnapshot {
            config.validate()
            return ServerConfigSnapshot(config)
        }

        fun fromSystemProperties(source: ConfigurationSource): ServerConfigSnapshot =
            from(ServerConfig.fromSystemProperties(source)).copy(provenance = ConfigurationProvenance.from(source))
    }

    fun withRevision(nextRevision: Long): ServerConfigSnapshot = copy(revision = nextRevision)
}
