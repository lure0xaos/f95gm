package f95gm.server.state.runtime

import f95gm.server.config.ServerConfigurationState
import f95gm.server.config.ServerConfigSnapshot
import f95gm.server.config.ConfigurationReloadResult
import f95gm.server.config.ConfigurationSource

/** Runtime-facing configuration API; policy consumers subscribe through the state object. */
internal class ServerConfigurationController(val state: ServerConfigurationState) {
    fun current(): ServerConfigSnapshot = state.current()
    fun reload(source: ConfigurationSource): ConfigurationReloadResult = state.reloadWithResult(source)
}
