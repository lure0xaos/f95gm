package f95gm.client.actions.marks.version

import f95gm.client.actions.marks.loading.MarkCheckNotification
import f95gm.client.actions.marks.loading.checkMarkedGamesInternal
import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.state.runtime.launchVersionCheckOperation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

internal fun startVersionCheckLoop(runtime: ClientMarksActionsPort) {
    if (runtime.requests.isJobActive(ClientJobKey.VERSION_CHECK)) return
    runtime.launchVersionCheckOperation {
        combine(
            runtime.session.data,
            runtime.appSettingsView.data
        ) { sessionState, settings -> sessionState.authenticated to settings.updateCheckInterval.duration }
            .collectLatest { (authenticated, interval) ->
                if (!authenticated) return@collectLatest
                runtime.clock.delay(if (interval.isPositive()) interval else 60.seconds)
                if (runtime.session.data.first().authenticated &&
                    runtime.appSettingsView.data.first().updateCheckInterval.duration == interval && interval.isPositive()
                ) {
                    checkMarkedGamesInternal(runtime, MarkCheckNotification.SHOW_TOAST)
                }
            }
    }
}

internal fun stopVersionCheckLoop(runtime: ClientMarksActionsPort) {
    runtime.requests.cancelJob(ClientJobKey.VERSION_CHECK)
}
