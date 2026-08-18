package f95gm.client.ui.pages.latestupdates

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.latestUpdatesRoute
import f95gm.client.ui.pages.detailPage

internal fun RenderContext.latestUpdatesDetailPage(threadId: String) = detailPage(threadId, latestUpdatesRoute)
