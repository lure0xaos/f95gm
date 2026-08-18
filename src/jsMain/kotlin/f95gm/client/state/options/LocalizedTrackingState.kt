package f95gm.client.state.options

import f95gm.domain.tracking.GameTrackingState

internal fun localizedTrackingState(state: GameTrackingState): String =
    trackingStateOptions.firstOrNull { it.first == state }?.second ?: state.wireValue
