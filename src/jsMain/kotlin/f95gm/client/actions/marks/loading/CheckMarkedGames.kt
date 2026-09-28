package f95gm.client.actions.marks.loading

import f95gm.client.state.runtime.ClientMarksActionsPort

internal fun checkMarkedGames(runtime: ClientMarksActionsPort, notification: MarkCheckNotification) {
    launchMarkedGamesCheck(runtime, notification)
}
