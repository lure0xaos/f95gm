package f95gm.client.state.session

import dev.fritz2.core.RootStore
import f95gm.client.model.auth.Credentials
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.Job

internal val credentials = RootStore(Credentials(F95Defaults.EMPTY, F95Defaults.EMPTY), Job())
