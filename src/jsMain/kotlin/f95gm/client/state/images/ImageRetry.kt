package f95gm.client.state.images

import dev.fritz2.core.RootStore
import f95gm.client.model.images.ImageRetryState
import kotlinx.coroutines.Job

internal val imageRetry = RootStore(ImageRetryState(), Job())
