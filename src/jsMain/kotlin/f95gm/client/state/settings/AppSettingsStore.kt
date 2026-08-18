package f95gm.client.state.settings

import dev.fritz2.core.RootStore
import kotlinx.coroutines.Job

internal val appSettings = RootStore(loadAppSettings(), Job())
