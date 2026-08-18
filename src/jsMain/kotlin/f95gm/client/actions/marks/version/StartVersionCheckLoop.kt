package f95gm.client.actions.marks.version

import f95gm.client.actions.marks.loading.checkMarkedGamesInternal
import f95gm.client.state.marks.versionCheckLoopStarted
import f95gm.client.state.runtime.scope
import f95gm.client.state.session.session
import f95gm.client.state.settings.appSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun startVersionCheckLoop() {
    if (versionCheckLoopStarted) return
    versionCheckLoopStarted = true
    scope.launch {
        while (session.data.first().authenticated) {
            val interval = appSettings.data.first().updateCheckInterval.millis
            if (interval > 0) {
                delay(interval.milliseconds)
                if (session.data.first().authenticated && appSettings.data.first().updateCheckInterval.millis == interval) {
                    checkMarkedGamesInternal(notify = true)
                }
            } else {
                delay(60_000L.milliseconds)
            }
        }
        versionCheckLoopStarted = false
    }
}
