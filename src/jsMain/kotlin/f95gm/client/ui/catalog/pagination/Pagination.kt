package f95gm.client.ui.catalog.pagination

import dev.fritz2.core.RenderContext
import f95gm.client.ui.shared.pagination.PaginationState
import f95gm.client.ui.shared.pagination.paginationControls
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.client.model.catalog.loading

internal fun RenderContext.pagination(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    paginationControls(
        source = catalog.data,
        stateFor = { state ->
            PaginationState(state.page, state.totalPages, state.loading, state.editingPage, state.pageInput)
        },
        onFirstPage = { runtime.catalogCommands.movePageTo(1) },
        onPreviousPage = { runtime.catalogCommands.movePage(-1) },
        onPage = { runtime.catalogCommands.movePageTo(it) },
        onNextPage = { runtime.catalogCommands.movePage(1) },
        onLastPage = { runtime.catalogCommands.movePageTo(Int.MAX_VALUE) },
        onStartPageEdit = { runtime.catalogCommands.startCatalogPageEdit() },
        onPageInput = { runtime.catalogCommands.updateCatalogPageInput(it) },
        onSubmitPageInput = { runtime.catalogCommands.submitCatalogPageInput() },
        onClosePageEdit = { runtime.catalogCommands.closeCatalogPageEdit() }
    )
}
