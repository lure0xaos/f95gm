package f95gm.client.ui.shared.pagination

import dev.fritz2.core.*
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal fun <T> RenderContext.paginationControls(
    source: Flow<T>,
    stateFor: (T) -> PaginationState,
    onFirstPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onPage: (Int) -> Unit,
    onNextPage: () -> Unit,
    onLastPage: () -> Unit,
    onStartPageEdit: () -> Unit,
    onPageInput: (String) -> Unit,
    onSubmitPageInput: () -> Unit,
    onClosePageEdit: () -> Unit
) {
    div("pager d-flex justify-content-center mt-4") {
        source.render { rawState ->
            val state = stateFor(rawState)
            val firstVisiblePage = maxOf(
                F95Defaults.FIRST_PAGE,
                state.page - PAGINATION_BUTTONS_EACH_SIDE
            )
            val lastVisiblePage = minOf(
                state.totalPages,
                state.page + PAGINATION_BUTTONS_EACH_SIDE
            )

            div("pager-group btn-group") {
                pageIconButton(
                    iconClass = "bi bi-chevron-double-left",
                    label = UiMessages.app_firstPage(),
                    disabled = state.page <= F95Defaults.FIRST_PAGE || state.loading,
                    onClick = onFirstPage
                )
                pageIconButton(
                    iconClass = "bi bi-chevron-left",
                    label = UiMessages.app_previousPage(),
                    disabled = state.page <= F95Defaults.FIRST_PAGE || state.loading,
                    onClick = onPreviousPage
                )

                (firstVisiblePage..lastVisiblePage).forEach { page ->
                    if (page == state.page) {
                        if (state.editingPage) {
                            input("page-input $textFieldClass text-center fw-semibold") {
                                type("number")
                                attr("min", F95Defaults.FIRST_PAGE.toString())
                                attr("max", state.totalPages.toString())
                                attr("inputmode", "numeric")
                                attr("aria-label", UiMessages.app_pageOf(state.page, state.totalPages))
                                attr("autofocus", "true")
                                attr("style", "width: 4rem")
                                value(flowOf(state.pageInput))
                                inputs.values() handledBy onPageInput
                                keyupsIf { key == "Enter" } handledBy {
                                    onSubmitPageInput()
                                }
                                blurs handledBy { onClosePageEdit() }
                            }
                        } else {
                            button("page-current $primaryNavigationButtonClass") {
                                type("button")
                                attr("aria-label", UiMessages.app_pageOf(state.page, state.totalPages))
                                title(UiMessages.app_pageOf(state.page, state.totalPages))
                                +page.toString()
                                clicks handledBy { onStartPageEdit() }
                            }
                        }
                    } else {
                        button("page-number $secondaryNavigationButtonClass") {
                            type("button")
                            attr("aria-label", UiMessages.app_pageOf(page, state.totalPages))
                            title(UiMessages.app_pageOf(page, state.totalPages))
                            +page.toString()
                            disabled(state.loading)
                            clicks handledBy { onPage(page) }
                        }
                    }
                }

                pageIconButton(
                    iconClass = "bi bi-chevron-right",
                    label = UiMessages.app_nextPage(),
                    disabled = state.page >= state.totalPages || state.loading,
                    onClick = onNextPage
                )
                pageIconButton(
                    iconClass = "bi bi-chevron-double-right",
                    label = UiMessages.app_lastPage(),
                    disabled = state.page >= state.totalPages || state.loading,
                    onClick = onLastPage
                )
            }
        }
    }
}
