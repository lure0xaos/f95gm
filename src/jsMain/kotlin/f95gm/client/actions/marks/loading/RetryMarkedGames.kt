package f95gm.client.actions.marks.loading

import f95gm.client.state.runtime.ClientMarksActionsPort

internal fun retryMarkedGames(runtime: ClientMarksActionsPort) {
    launchMarkedGamesCheck(runtime, MarkCheckNotification.SILENT)
}
