package f95gm.client.actions.runtime

import f95gm.client.data.api.apiResult
import f95gm.client.data.api.requestJvmHealth
import f95gm.client.state.runtime.BrowserTabId
import f95gm.client.state.settings.appSettings
import kotlinx.coroutines.flow.first

internal suspend fun checkJvmConnection() {
    apiResult {
        val theme = appSettings.data.first().theme.desktopValue()
        requestJvmHealth(healthProbeNumber++, BrowserTabId.value, theme)
    }
        .onSuccess(::recordJvmHealth)
        .onFailure { recordJvmUnavailable() }
}
