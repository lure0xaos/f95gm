package f95gm.server.config

internal class ServerConfigSnapshot private constructor(val value: ServerConfig) {
    companion object {
        fun from(config: ServerConfig): ServerConfigSnapshot {
            config.validate()
            return ServerConfigSnapshot(config)
        }

        fun fromSystemProperties(): ServerConfigSnapshot = from(ServerConfig.fromSystemProperties())
    }
}
