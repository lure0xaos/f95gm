package f95gm.client.state.detail

import dev.fritz2.core.RootStore
import f95gm.client.model.detail.DetailState
import kotlinx.coroutines.Job

internal val detail = RootStore(DetailState(), Job())
