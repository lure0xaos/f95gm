package f95gm.client.actions.marks.loading

import f95gm.client.actions.marks.version.startVersionCheckLoop
import f95gm.client.state.marks.loadedMarks

internal fun loadMarksIfNeeded() {
    if (loadedMarks) return
    loadedMarks = true
    launchMarkedGamesCheck(MarkCheckNotification.SHOW_TOAST)
    startVersionCheckLoop()
}
