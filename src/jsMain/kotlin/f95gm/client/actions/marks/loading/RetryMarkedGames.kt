package f95gm.client.actions.marks.loading

import f95gm.client.state.runtime.scope
import kotlinx.coroutines.launch

internal fun retryMarkedGames() {
    scope.launch { checkMarkedGamesInternal(notify = false) }
}
