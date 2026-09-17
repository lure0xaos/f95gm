package f95gm.client.ui.pages.mygames

import dev.fritz2.core.RenderContext
import f95gm.client.routing.navigation.myGamesRoute
import f95gm.client.ui.pages.detailPage
import f95gm.client.state.runtime.ClientDetailPagePort
import f95gm.domain.values.F95ThreadId

internal fun RenderContext.myGamesDetailPage(runtime: ClientDetailPagePort, threadId: F95ThreadId) =
    detailPage(runtime, threadId, myGamesRoute)
