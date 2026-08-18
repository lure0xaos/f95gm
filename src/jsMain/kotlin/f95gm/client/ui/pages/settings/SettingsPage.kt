package f95gm.client.ui.pages.settings

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.actions.filters.exportSavedFilters
import f95gm.client.actions.filters.importSavedFilters
import f95gm.client.actions.settings.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.client.state.settings.appSettings
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.settingsPage() {
    fun themeLabel(theme: AppTheme): String = when (theme) {
        AppTheme.DARK -> UiMessages.app_themeDark()
        AppTheme.LIGHT -> UiMessages.app_themeLight()
        AppTheme.SYSTEM -> UiMessages.app_themeSystem()
    }

    fun intervalLabel(interval: UpdateCheckInterval): String = when (interval) {
        UpdateCheckInterval.DISABLED -> UiMessages.app_updatesDisabled()
        UpdateCheckInterval.FIFTEEN_MINUTES -> UiMessages.app_every15Minutes()
        UpdateCheckInterval.HOUR -> UiMessages.app_everyHour()
        UpdateCheckInterval.SIX_HOURS -> UiMessages.app_every6Hours()
        UpdateCheckInterval.DAY -> UiMessages.app_everyDay()
    }

    fun pageSizeLabel(size: PageSize): String = "${size.value}"

    fun cardSizeLabel(size: CardSize): String = when (size) {
        CardSize.VERY_COMPACT -> UiMessages.app_cardSizeVeryCompact()
        CardSize.COMPACT -> UiMessages.app_cardSizeCompact()
        CardSize.DEFAULT -> UiMessages.app_cardSizeDefault()
        CardSize.LARGE -> UiMessages.app_cardSizeLarge()
        CardSize.VERY_LARGE -> UiMessages.app_cardSizeVeryLarge()
    }

    div("settings-page") {
        responsiveNavbar(
            navigationId = "settings-navigation",
            catalogLink = true,
            toolbar = {}
        )
        div("settings-content container-xl py-4") {
            h1("h3 mb-1") { +UiMessages.app_settings() }
            p("text-body-secondary mb-4") { +UiMessages.app_settingsHint() }

            div("settings-section card border-0 shadow-sm p-4") {
                h2("h5 mb-3") { +UiMessages.app_appearance() }
                div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3") {
                    div("settings-copy") {
                        div("fw-semibold") { +UiMessages.app_theme() }
                    }
                    bootstrapDropdown(
                        selected = appSettings.data.map { it.theme.bootstrapValue },
                        selectedLabel = appSettings.data.map { themeLabel(it.theme) },
                        options = flowOf(
                            listOf(
                                DropdownChoice(AppTheme.DARK.bootstrapValue, UiMessages.app_themeDark()),
                                DropdownChoice(AppTheme.LIGHT.bootstrapValue, UiMessages.app_themeLight()),
                                DropdownChoice(AppTheme.SYSTEM.bootstrapValue, UiMessages.app_themeSystem())
                            )
                        ),
                        buttonClass = secondaryNavigationButtonClass,
                        widthClass = "w-auto"
                    ) { selectAppTheme(it) }
                }
            }

            div("settings-section card border-0 shadow-sm p-4 mt-3") {
                h2("h5 mb-3") { +UiMessages.app_images() }
                div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3") {
                    div("settings-copy") {
                        div("fw-semibold") { +UiMessages.app_lazyLoading() }
                        p("text-body-secondary small mb-0") { +UiMessages.app_lazyLoadingHint() }
                    }
                    button(secondaryNavigationButtonClass) {
                        type("button")
                        appSettings.data.map {
                            if (it.lazyLoadImages) UiMessages.app_enabled() else UiMessages.app_disabled()
                        }.render { +it }
                        clicks handledBy { toggleLazyImageLoading() }
                    }
                }
            }

            div("settings-section card border-0 shadow-sm p-4 mt-3") {
                h2("h5 mb-3") { +UiMessages.app_catalogDisplay() }
                div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3") {
                    div("settings-copy") {
                        div("fw-semibold") { +UiMessages.app_pageSize() }
                        p("text-body-secondary small mb-0") { +UiMessages.app_pageSizeHint() }
                    }
                    bootstrapDropdown(
                        selected = appSettings.data.map { it.pageSize.value.toString() },
                        selectedLabel = appSettings.data.map { pageSizeLabel(it.pageSize) },
                        options = flowOf(PageSize.entries.map {
                            DropdownChoice(
                                it.value.toString(),
                                pageSizeLabel(it)
                            )
                        }),
                        buttonClass = secondaryNavigationButtonClass,
                        widthClass = "w-auto"
                    ) { selectPageSize(it) }
                }
                div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3 mt-3") {
                    div("settings-copy") {
                        div("fw-semibold") { +UiMessages.app_cardSize() }
                        p("text-body-secondary small mb-0") { +UiMessages.app_cardSizeHint() }
                    }
                    bootstrapDropdown(
                        selected = appSettings.data.map { it.cardSize.value },
                        selectedLabel = appSettings.data.map { cardSizeLabel(it.cardSize) },
                        options = flowOf(CardSize.entries.map { DropdownChoice(it.value, cardSizeLabel(it)) }),
                        buttonClass = secondaryNavigationButtonClass,
                        widthClass = "w-auto"
                    ) { selectCardSize(it) }
                }
            }

            div("settings-section card border-0 shadow-sm p-4 mt-3") {
                h2("h5 mb-3") { +UiMessages.app_updateChecks() }
                div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3") {
                    div("settings-copy") {
                        p("text-body-secondary small mb-0") { +UiMessages.app_updateChecksHint() }
                    }
                    bootstrapDropdown(
                        selected = appSettings.data.map { it.updateCheckInterval.millis.toString() },
                        selectedLabel = appSettings.data.map { intervalLabel(it.updateCheckInterval) },
                        options = flowOf(
                            listOf(
                                DropdownChoice(
                                    UpdateCheckInterval.DISABLED.millis.toString(),
                                    UiMessages.app_updatesDisabled()
                                ),
                                DropdownChoice(
                                    UpdateCheckInterval.FIFTEEN_MINUTES.millis.toString(),
                                    UiMessages.app_every15Minutes()
                                ),
                                DropdownChoice(UpdateCheckInterval.HOUR.millis.toString(), UiMessages.app_everyHour()),
                                DropdownChoice(
                                    UpdateCheckInterval.SIX_HOURS.millis.toString(),
                                    UiMessages.app_every6Hours()
                                ),
                                DropdownChoice(UpdateCheckInterval.DAY.millis.toString(), UiMessages.app_everyDay())
                            )
                        ),
                        buttonClass = secondaryNavigationButtonClass,
                        widthClass = "w-auto"
                    ) { selectUpdateCheckInterval(it) }
                }
            }

            div("settings-section card border-0 shadow-sm p-4 mt-3") {
                h2("h5 mb-2") { +UiMessages.app_data() }
                p("text-body-secondary small mb-3") { +UiMessages.app_dataHint() }
                div("d-flex flex-wrap gap-2") {
                    button(secondaryNavigationButtonClass) {
                        type("button")
                        span("bi bi-download me-1") {}
                        +UiMessages.app_exportQuickLinks()
                        clicks handledBy { exportSavedFilters() }
                    }
                    button(secondaryNavigationButtonClass) {
                        type("button")
                        span("bi bi-upload me-1") {}
                        +UiMessages.app_importQuickLinks()
                        clicks handledBy { importSavedFilters() }
                    }
                }
            }
        }
    }
}
