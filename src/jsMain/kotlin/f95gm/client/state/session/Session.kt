package f95gm.client.state.session

import dev.fritz2.core.RootStore
import f95gm.client.model.auth.SessionState
import kotlinx.coroutines.Job

internal val session = RootStore(SessionState(checking = true), Job())
