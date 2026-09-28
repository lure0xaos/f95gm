package f95gm.client.actions.marks.loading

import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.client.state.runtime.launchMarkedGamesCheckOperation

internal fun launchMarkedGamesCheck(runtime: ClientMarksActionsPort, notification: MarkCheckNotification) {
    runtime.launchMarkedGamesCheckOperation { checkMarkedGamesInternal(runtime, notification) }
}

internal fun cancelMarkedGamesCheck(runtime: ClientMarksActionsPort) {
    runtime.requests.cancelJob(ClientJobKey.MARKED_GAMES_CHECK)
}
