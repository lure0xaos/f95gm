package f95gm.client.actions.marks.loading

import f95gm.client.actions.marks.version.startVersionCheckLoop
import f95gm.client.state.runtime.ClientMarksActionsPort

internal fun loadMarksIfNeeded(runtime: ClientMarksActionsPort) {
    if (runtime.marksLoading.isLoaded()) return
    runtime.marksLoading.markLoaded()
    launchMarkedGamesCheck(runtime, MarkCheckNotification.SHOW_TOAST)
    startVersionCheckLoop(runtime)
}
