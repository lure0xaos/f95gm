package f95gm.client.state.runtime

import dev.fritz2.core.RootStore
import kotlinx.coroutines.Job

internal val jvmConnection = RootStore(JvmConnectionState(), Job())
