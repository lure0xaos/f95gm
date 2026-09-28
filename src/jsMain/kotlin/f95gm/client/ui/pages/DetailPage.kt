package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import dev.fritz2.core.alt
import dev.fritz2.core.src
import dev.fritz2.core.type
import f95gm.client.model.detail.DetailBodyState
import f95gm.client.routing.navigation.TypedRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.config.ClientConstants
import f95gm.client.ui.detail.metadata.detailBadgeMeta
import f95gm.client.ui.detail.metadata.detailGenres
import f95gm.client.ui.detail.metadata.detailMarkControls
import f95gm.client.ui.detail.metadata.detailMetaCards
import f95gm.client.ui.detail.renderDetailBody
import f95gm.client.ui.navigation.*
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientDetailPagePort
import f95gm.domain.values.F95ThreadId
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun RenderContext.detailPage(runtime: ClientDetailPagePort, threadId: F95ThreadId, backRoute: TypedRoute) {
    val detail = runtime.detailView
    div("detail-page container-xl") {
        detailPageHeader(runtime)
        div("detail-actions d-flex justify-content-between align-items-center gap-3 mt-4") {
            a("back-link $secondaryNavigationButtonClass") {
                clicks.map { backRoute } handledBy router.navTo
                span("bi bi-arrow-left me-1") {}
                +UiMessages.app_back()
            }
            a("external-link $secondaryNavigationButtonClass") {
                attr("href", "https://f95zone.to/threads/${threadId.value}/")
                attr("target", "_blank")
                attr("rel", "noopener noreferrer")
                span("bi bi-box-arrow-up-right me-1") {}
                +UiMessages.app_openF95zoneThread()
            }
        }
        detailPageContent(runtime, threadId)
    }
    runtime.detailCommands.loadDetailIfNeeded(threadId)
    runtime.filtersCommands.loadSavedFiltersIfNeeded()
}
