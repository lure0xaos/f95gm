package f95gm.client.actions.runtime

import f95gm.client.state.runtime.BrowserTabId
import f95gm.client.state.runtime.http
import f95gm.client.state.settings.appSettings
import f95gm.domain.api.F95Api
import io.ktor.client.request.*
import kotlinx.coroutines.flow.first

internal suspend fun checkJvmConnection() {
    runCatching {
        val theme = appSettings.data.first().theme.desktopValue()
        http.get(
            "${F95Api.HEALTH}?probe=${healthProbeNumber++}" +
                    "&${F95Api.TAB_ID}=${BrowserTabId.value}&${F95Api.THEME}=$theme"
        ).status.value
    }
        .onSuccess(::recordJvmHealth)
        .onFailure { recordJvmUnavailable() }
}
