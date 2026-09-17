package f95gm.server.routes.marks

import f95gm.server.config.filters.gameTrackingStates
import f95gm.domain.api.F95MarkRequest

internal fun F95MarkRequest.hasValidTrackingState(): Boolean =
    trackingState?.wireValue?.takeIf { it in gameTrackingStates } != null

