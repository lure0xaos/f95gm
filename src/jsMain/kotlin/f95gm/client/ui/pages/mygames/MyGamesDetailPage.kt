package f95gm.client.ui.pages.mygames

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.myGamesRoute
import f95gm.client.ui.pages.detailPage

internal fun RenderContext.myGamesDetailPage(threadId: String) = detailPage(threadId, myGamesRoute)
