package f95gm.client.state.filters

import dev.fritz2.core.RootStore
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.Job

internal val savedFilterName = RootStore(F95Defaults.EMPTY, Job())
