package f95gm.client.actions.marks.version

import f95gm.client.actions.marks.loading.MarkCheckNotification
import f95gm.client.actions.marks.loading.checkMarkedGamesInternal
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.clientController
import f95gm.client.state.runtime.launchClientOperation
import f95gm.client.state.session.session
import f95gm.client.state.settings.appSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds

internal fun startVersionCheckLoop() {
    if (clientController.isJobActive(ClientJobKey.VERSION_CHECK)) return
    launchClientOperation(ClientJobKey.VERSION_CHECK) {
        combine(
            session.data,
            appSettings.data
        ) { sessionState, settings -> sessionState.authenticated to settings.updateCheckInterval.millis }
            .collectLatest { (authenticated, interval) ->
                if (!authenticated) return@collectLatest
                delay(if (interval > 0) interval.milliseconds else 60_000L.milliseconds)
                if (session.data.first().authenticated && appSettings.data.first().updateCheckInterval.millis == interval && interval > 0) {
                    checkMarkedGamesInternal(MarkCheckNotification.SHOW_TOAST)
                }
            }
    }
}

internal fun stopVersionCheckLoop() {
    clientController.cancelJob(ClientJobKey.VERSION_CHECK)
}
