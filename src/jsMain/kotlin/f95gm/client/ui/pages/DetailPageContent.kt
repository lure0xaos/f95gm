package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import dev.fritz2.core.alt
import dev.fritz2.core.src
import dev.fritz2.core.type
import f95gm.client.model.detail.DetailBodyState
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.config.ClientConstants
import f95gm.client.model.detail.loading
import f95gm.client.model.detail.error
import f95gm.client.ui.detail.metadata.detailBadgeMeta
import f95gm.client.ui.detail.metadata.detailGenres
import f95gm.client.ui.detail.metadata.detailMarkControls
import f95gm.client.ui.detail.metadata.detailMetaCards
import f95gm.client.ui.detail.renderDetailBody
import f95gm.client.ui.navigation.*
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientDetailUiPort
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.renderDetailPageContent(runtime: ClientDetailUiPort, threadId: F95ThreadId) =
    renderDetailPageSections(runtime, threadId)
