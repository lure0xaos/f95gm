package f95gm.client.ui.catalog.pagination

import dev.fritz2.core.RenderContext
import f95gm.client.actions.catalog.pagination.*
import f95gm.client.state.catalog.catalog
import f95gm.client.ui.shared.pagination.PaginationState
import f95gm.client.ui.shared.pagination.paginationControls

internal fun RenderContext.pagination() {
    paginationControls(
        source = catalog.data,
        stateFor = { state ->
            PaginationState(state.page, state.totalPages, state.loading, state.editingPage, state.pageInput)
        },
        onFirstPage = { movePageTo(1) },
        onPreviousPage = { movePage(-1) },
        onPage = { movePageTo(it) },
        onNextPage = { movePage(1) },
        onLastPage = { movePageTo(Int.MAX_VALUE) },
        onStartPageEdit = { startCatalogPageEdit() },
        onPageInput = { updateCatalogPageInput(it) },
        onSubmitPageInput = { submitCatalogPageInput() },
        onClosePageEdit = { closeCatalogPageEdit() }
    )
}
