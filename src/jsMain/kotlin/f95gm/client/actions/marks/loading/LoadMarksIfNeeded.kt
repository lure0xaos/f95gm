package f95gm.client.actions.marks.loading

import f95gm.client.actions.marks.version.startVersionCheckLoop
import f95gm.client.state.marks.loadedMarks
import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun loadMarksIfNeeded() {
    if (loadedMarks) return
    loadedMarks = true
    scope.launch { checkMarkedGamesInternal(notify = true) }
    startVersionCheckLoop()
}
