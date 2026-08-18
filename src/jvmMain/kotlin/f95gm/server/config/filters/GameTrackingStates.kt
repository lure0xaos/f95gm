package f95gm.server.config.filters

import f95gm.domain.tracking.GameTrackingState

internal val gameTrackingStates = GameTrackingState.entries.map { it.wireValue }.toSet()
