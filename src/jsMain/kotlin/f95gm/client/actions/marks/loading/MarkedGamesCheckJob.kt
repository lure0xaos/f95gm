package f95gm.client.actions.marks.loading

import f95gm.client.state.runtime.ClientJobKey
import f95gm.client.state.runtime.clientController
import f95gm.client.state.runtime.launchClientOperation

internal fun launchMarkedGamesCheck(notification: MarkCheckNotification) {
    launchClientOperation(ClientJobKey.MARKED_GAMES_CHECK) { checkMarkedGamesInternal(notification) }
}

internal fun cancelMarkedGamesCheck() {
    clientController.cancelJob(ClientJobKey.MARKED_GAMES_CHECK)
}
