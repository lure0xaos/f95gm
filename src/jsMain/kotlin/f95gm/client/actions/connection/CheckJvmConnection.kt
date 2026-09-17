package f95gm.client.actions.connection

import f95gm.client.data.api.apiResult
import f95gm.client.state.runtime.ClientConnectionActionsPort
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.ApiBrowserTabId
import f95gm.domain.api.ApiProbeNumber
import f95gm.domain.api.ApiTheme
import kotlinx.coroutines.flow.first

internal suspend fun checkJvmConnection(runtime: ClientConnectionActionsPort) {
    apiResult {
        val theme = runtime.appSettingsView.data.first().theme.desktopValue(runtime.browser)
        runtime.healthApi.health(
            F95ApiPaths.healthPath(
                ApiProbeNumber(runtime.healthProbes.nextProbeNumber()),
                ApiBrowserTabId(runtime.browserTabId.value),
                ApiTheme(theme)
            )
        ).status
    }
        .onSuccess { recordJvmHealth(runtime, it) }
        .onFailure { recordJvmUnavailable(runtime) }
}
