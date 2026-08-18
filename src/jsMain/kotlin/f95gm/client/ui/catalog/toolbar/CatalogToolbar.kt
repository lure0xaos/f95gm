package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.actions.catalog.filters.editing.applyFilters
import f95gm.client.actions.catalog.filters.editing.clearFilters
import f95gm.client.actions.catalog.filters.editing.toggleFilterPanel
import f95gm.client.actions.catalog.filters.search.updateCreatorSearch
import f95gm.client.actions.catalog.filters.selection.*
import f95gm.client.actions.catalog.loading.loadFilterOptionsIfNeeded
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.catalog.catalog
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.catalog.filterPanelOpen
import f95gm.client.state.catalog.loadedOptionsCategory
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.options.dateLimitChoices
import f95gm.client.state.options.dateLimitLabel
import f95gm.client.ui.catalog.filters.filterChip
import f95gm.client.ui.catalog.filters.filterField
import f95gm.client.ui.catalog.filters.filterPicker
import f95gm.client.ui.catalog.filters.prefixChip
import f95gm.client.ui.catalog.saved.savedFilterManager
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95TagType
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogToolbar() {
    responsiveNavbar(
        navigationId = "catalog-navigation",
        catalogLink = false,
        toolbar = { catalogToolbarNavigation() },
        trailingAction = {
            button("filter-toggle") {
                className(filterPanelOpen.data.map { isOpen ->
                    "filter-toggle ${if (isOpen) primaryNavigationButtonClass else navigationButtonClass}"
                })
                type("button")
                attr("data-bs-toggle", "collapse")
                attr("data-bs-target", "#catalog-filter-panel")
                attr("aria-controls", "catalog-filter-panel")
                attr("aria-expanded", "false")
                span("bi bi-funnel me-1") {}
                +UiMessages.app_filters()
                clicks handledBy { toggleFilterPanel() }
            }
        }
    )
    div("filter-panel collapse w-100 mt-2") {
        id("catalog-filter-panel")
        div("filter-content border-top pt-3") {
            div("filter-heading d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3") {
                div("filter-intro") {
                    div("filter-title text-uppercase text-primary small fw-semibold") { +UiMessages.app_filterFeed() }
                    p("filter-hint small text-body-secondary mb-0 mt-1") { +UiMessages.app_filterHint() }
                }
                div("filter-actions d-flex flex-wrap align-items-center justify-content-end gap-2") {
                    button("filter-clear $secondaryNavigationButtonClass") {
                        type("button")
                        span("bi bi-x-lg me-1") {}
                        +UiMessages.app_clear()
                        clicks handledBy { clearFilters() }
                    }
                    button("filter-apply $primaryNavigationButtonClass") {
                        type("button")
                        span("bi bi-funnel me-1") {}
                        +UiMessages.app_applyFilters()
                        clicks handledBy { applyFilters() }
                    }
                }
            }
            catalogOptions.data.render { options ->
                div("filter-grid row row-cols-1 row-cols-xl-3 g-3 align-items-stretch") {
                    div("col d-flex") {
                        div("filter-card card p-3 w-100 h-100 d-flex flex-column") {
                            div("filter-title text-uppercase text-primary small fw-semibold mb-3") { +UiMessages.app_search() }
                            div("row row-cols-1 g-3 align-content-start flex-grow-1") {
                                div("col") {
                                    filterField(UiMessages.app_searchIn()) {
                                        bootstrapDropdown(
                                            selected = catalog.data.map { it.searchMode.wireValue },
                                            selectedLabel = catalog.data.map { if (it.searchMode == F95SearchMode.CREATOR) UiMessages.app_creators() else UiMessages.app_titles() },
                                            options = flowOf(
                                                listOf(
                                                    DropdownChoice(
                                                        F95SearchMode.TITLE.wireValue,
                                                        UiMessages.app_titles()
                                                    ),
                                                    DropdownChoice(
                                                        F95SearchMode.CREATOR.wireValue,
                                                        UiMessages.app_creators()
                                                    )
                                                )
                                            ),
                                            buttonClass = "$secondaryNavigationButtonClass text-start",
                                            searchKey = ClientConstants.SEARCH_IN_KEY
                                        ) { selectSearchMode(it) }
                                    }
                                }
                                div("col") {
                                    filterField(UiMessages.app_creatorSearch()) {
                                        input("$textFieldClass w-100") {
                                            type("search")
                                            placeholder(UiMessages.app_searchCreatorsPlaceholder())
                                            value(catalog.data.map { it.creatorSearch })
                                            inputs.values() handledBy { updateCreatorSearch(it) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    div("col d-flex") {
                        div("filter-card card p-3 w-100 h-100 d-flex flex-column") {
                            div("filter-title text-uppercase text-primary small fw-semibold mb-3") { +UiMessages.app_timeAndTagMatching() }
                            div("row row-cols-1 g-3 align-content-start flex-grow-1") {
                                div("col") {
                                    filterField(UiMessages.app_dateLimit()) {
                                        bootstrapDropdown(
                                            selected = catalog.data.map { it.dateLimit.toString() },
                                            selectedLabel = catalog.data.map { dateLimitLabel(it.dateLimit) },
                                            options = flowOf(dateLimitChoices),
                                            buttonClass = "$secondaryNavigationButtonClass text-start",
                                            searchKey = ClientConstants.DATE_LIMIT_KEY
                                        ) { selectDateLimit(it) }
                                    }
                                }
                                div("col") {
                                    filterField(UiMessages.app_tagMatching()) {
                                        bootstrapDropdown(
                                            selected = catalog.data.map { it.tagType.wireValue },
                                            selectedLabel = catalog.data.map { if (it.tagType == F95TagType.AND) UiMessages.app_matchEveryTag() else UiMessages.app_matchAnyTag() },
                                            options = flowOf(
                                                listOf(
                                                    DropdownChoice(
                                                        F95TagType.OR.wireValue,
                                                        UiMessages.app_matchAnyTag()
                                                    ),
                                                    DropdownChoice(
                                                        F95TagType.AND.wireValue,
                                                        UiMessages.app_matchEveryTag()
                                                    )
                                                )
                                            ),
                                            buttonClass = "$secondaryNavigationButtonClass text-start",
                                            searchKey = "tagMatching"
                                        ) { selectTagType(it) }
                                    }
                                }
                            }
                        }
                    }
                    div("col d-flex") {
                        div("filter-card card p-3 w-100 h-100 d-flex flex-column") {
                            div("filter-title text-uppercase text-primary small fw-semibold mb-3") { +UiMessages.app_tags() }
                            div("row row-cols-1 g-3 align-content-start flex-grow-1") {
                                filterPicker(
                                    fieldLabel = UiMessages.app_includeTags(),
                                    options = catalogOptions.data.map { it.tags },
                                    selected = catalog.data.map { it.tagPicker },
                                    highlighted = catalog.data.map { state ->
                                        state.selectedTags.map { it.id.value }.toSet()
                                    },
                                    searchKey = "includeTags",
                                    onChange = { selectTagPicker(it) }
                                )
                                filterPicker(
                                    fieldLabel = UiMessages.app_excludeTags(),
                                    options = catalogOptions.data.map { it.tags },
                                    selected = catalog.data.map { it.excludedTagPicker },
                                    highlighted = catalog.data.map { state ->
                                        state.excludedTags.map { it.id.value }.toSet()
                                    },
                                    searchKey = "excludeTags",
                                    onChange = { selectExcludedTagPicker(it) }
                                )
                            }
                        }
                    }
                    options.prefixGroups.forEach { group ->
                        div("col d-flex") {
                            div("filter-card card p-3 w-100 h-100 d-flex flex-column") {
                                div("filter-title text-uppercase text-primary small fw-semibold mb-3") { +group.name.value }
                                div("row row-cols-1 g-3 align-content-start flex-grow-1") {
                                    filterPicker(
                                        fieldLabel = UiMessages.app_includeGroup(group.name.value),
                                        options = flowOf(group.options),
                                        selected = catalog.data.map { it.prefixPickers[group.id].orEmpty() },
                                        highlighted = catalog.data.map { state ->
                                            state.selectedPrefixes.map { it.id.value }.toSet()
                                        },
                                        searchKey = "includePrefix:${group.id.value}",
                                        onChange = { selectPrefixPicker(it, group.id) }
                                    )
                                    filterPicker(
                                        fieldLabel = UiMessages.app_excludeGroup(group.name.value),
                                        options = flowOf(group.options),
                                        selected = catalog.data.map { it.excludedPrefixPickers[group.id].orEmpty() },
                                        highlighted = catalog.data.map { state ->
                                            state.excludedPrefixes.map { it.id.value }.toSet()
                                        },
                                        searchKey = "excludePrefix:${group.id.value}",
                                        onChange = { selectExcludedPrefixPicker(it, group.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            div("filter-chips d-flex flex-wrap align-items-center gap-2 mt-3") {
                catalog.data.map { it.selectedTags to it.excludedTags }.distinctUntilChanged()
                    .render { (included, excluded) ->
                        if (included.isNotEmpty() || excluded.isNotEmpty()) {
                            span("chip-label small text-uppercase text-body-secondary fw-semibold") { +UiMessages.app_tags() }
                            included.forEach { filterChip(it, false) }
                            excluded.forEach { filterChip(it, true) }
                        }
                    }
                catalog.data.map { it.selectedPrefixes to it.excludedPrefixes }.distinctUntilChanged()
                    .render { (included, excluded) ->
                        if (included.isNotEmpty() || excluded.isNotEmpty()) {
                            span("chip-label small text-uppercase text-body-secondary fw-semibold") { +UiMessages.app_prefixes() }
                            included.forEach { prefixChip(it, false) }
                            excluded.forEach { prefixChip(it, true) }
                        }
                    }
            }
            catalogOptions.data.map { it.loading to it.error }.distinctUntilChanged()
                .render { (loading, error) ->
                    if (loading) span("small text-body-secondary d-block mt-3") { +UiMessages.app_loadingChoices() }
                    else if (error.isNotBlank()) {
                        div("small text-danger d-flex flex-wrap align-items-center gap-2 mt-3") {
                            span { +error }
                            button(secondaryNavigationButtonClass) {
                                type("button")
                                +UiMessages.app_retry()
                                clicks handledBy {
                                    loadedOptionsCategory = null
                                    loadFilterOptionsIfNeeded()
                                }
                            }
                        }
                    }
                }
            savedFilterManager()
        }
    }
}
