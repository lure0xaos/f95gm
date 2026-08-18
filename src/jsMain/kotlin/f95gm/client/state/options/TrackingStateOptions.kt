package f95gm.client.state.options

import f95gm.domain.tracking.GameTrackingState
import f95gm.messages.UiMessages

internal val trackingStateOptions = listOf(
    GameTrackingState.WATCHING to UiMessages.app_trackingStateWatching(),
    GameTrackingState.DOWNLOADING to UiMessages.app_trackingStateDownloading(),
    GameTrackingState.DOWNLOADED to UiMessages.app_trackingStateDownloaded(),
    GameTrackingState.PLAYABLE to UiMessages.app_trackingStatePlayable(),
    GameTrackingState.PAUSED to UiMessages.app_trackingStatePaused(),
    GameTrackingState.COMPLETED to UiMessages.app_trackingStateCompleted()
)
