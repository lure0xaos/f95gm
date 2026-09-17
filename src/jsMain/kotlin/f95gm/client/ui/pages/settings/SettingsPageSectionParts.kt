package f95gm.client.ui.pages.settings

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.model.settings.AppTheme
import f95gm.client.model.settings.CardSize
import f95gm.client.model.settings.PageSize
import f95gm.client.model.settings.UpdateCheckInterval
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientSettingsUiPort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.settingsAppearanceSection(runtime: ClientSettingsUiPort) {
    val appSettings = runtime.appSettingsView
    div("settings-section card border-0 shadow-sm p-4") {
        h2("h5 mb-3") { +UiMessages.app_appearance() }
        div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3") {
            div("settings-copy") { div("fw-semibold") { +UiMessages.app_theme() } }
            bootstrapDropdown(
                runtime = runtime,
                selected = appSettings.data.map { it.theme },
                selectedLabel = appSettings.data.map { themeLabel(it.theme) },
                options = flowOf(listOf(
                    DropdownChoice(AppTheme.DARK, UiMessages.app_themeDark()),
                    DropdownChoice(AppTheme.LIGHT, UiMessages.app_themeLight()),
                    DropdownChoice(AppTheme.SYSTEM, UiMessages.app_themeSystem())
                )),
                buttonClass = secondaryNavigationButtonClass,
                widthClass = "w-auto"
            ) { runtime.settingsCommands.selectAppTheme(it) }
        }
    }
}

internal fun RenderContext.settingsImagesSection(runtime: ClientSettingsUiPort) {
    val appSettings = runtime.appSettingsView
    div("settings-section card border-0 shadow-sm p-4 mt-3") {
        h2("h5 mb-3") { +UiMessages.app_images() }
        div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3") {
            div("settings-copy") {
                div("fw-semibold") { +UiMessages.app_lazyLoading() }
                p("text-body-secondary small mb-0") { +UiMessages.app_lazyLoadingHint() }
            }
            button(secondaryNavigationButtonClass) {
                type("button")
                appSettings.data.map { if (it.lazyLoadImages) UiMessages.app_enabled() else UiMessages.app_disabled() }
                    .render { +it }
                clicks handledBy { runtime.settingsCommands.toggleLazyImageLoading() }
            }
        }
    }
}

internal fun RenderContext.settingsCatalogDisplaySection(runtime: ClientSettingsUiPort) {
    val appSettings = runtime.appSettingsView
    div("settings-section card border-0 shadow-sm p-4 mt-3") {
        h2("h5 mb-3") { +UiMessages.app_catalogDisplay() }
        settingsDropdownRow(runtime, UiMessages.app_pageSize(), UiMessages.app_pageSizeHint(),
            appSettings.data.map { it.pageSize },
            appSettings.data.map { pageSizeLabel(it.pageSize) },
            flowOf(PageSize.entries.map { DropdownChoice(it, pageSizeLabel(it)) })
        ) { runtime.settingsCommands.selectPageSize(it) }
        settingsDropdownRow(runtime, UiMessages.app_cardSize(), UiMessages.app_cardSizeHint(),
            appSettings.data.map { it.cardSize }, appSettings.data.map { cardSizeLabel(it.cardSize) },
            flowOf(CardSize.entries.map { DropdownChoice(it, cardSizeLabel(it)) })
        ) { runtime.settingsCommands.selectCardSize(it) }
    }
}

internal fun RenderContext.settingsUpdateSection(runtime: ClientSettingsUiPort) {
    val appSettings = runtime.appSettingsView
    div("settings-section card border-0 shadow-sm p-4 mt-3") {
        h2("h5 mb-3") { +UiMessages.app_updateChecks() }
        settingsDropdownRow(runtime, UiMessages.app_updateChecksHint(), null,
            appSettings.data.map { it.updateCheckInterval },
            appSettings.data.map { intervalLabel(it.updateCheckInterval) },
            flowOf(listOf(
                DropdownChoice(UpdateCheckInterval.DISABLED, UiMessages.app_updatesDisabled()),
                DropdownChoice(UpdateCheckInterval.FIFTEEN_MINUTES, UiMessages.app_every15Minutes()),
                DropdownChoice(UpdateCheckInterval.HOUR, UiMessages.app_everyHour()),
                DropdownChoice(UpdateCheckInterval.SIX_HOURS, UiMessages.app_every6Hours()),
                DropdownChoice(UpdateCheckInterval.DAY, UiMessages.app_everyDay())
            ))
        ) { runtime.settingsCommands.selectUpdateCheckInterval(it) }
    }
}

internal fun RenderContext.settingsDataSection(runtime: ClientSettingsUiPort) {
    div("settings-section card border-0 shadow-sm p-4 mt-3") {
        h2("h5 mb-2") { +UiMessages.app_data() }
        p("text-body-secondary small mb-3") { +UiMessages.app_dataHint() }
        div("d-flex flex-wrap gap-2") {
            button(secondaryNavigationButtonClass) {
                type("button"); span("bi bi-download me-1") {}; +UiMessages.app_exportQuickLinks()
                clicks handledBy { runtime.filtersCommands.exportSavedFilters() }
            }
            button(secondaryNavigationButtonClass) {
                type("button"); span("bi bi-upload me-1") {}; +UiMessages.app_importQuickLinks()
                clicks handledBy { runtime.filtersCommands.importSavedFilters() }
            }
        }
    }
}

internal fun <T> RenderContext.settingsDropdownRow(
    runtime: ClientSettingsUiPort,
    label: String,
    hint: String?,
    selected: kotlinx.coroutines.flow.Flow<T>,
    selectedLabel: kotlinx.coroutines.flow.Flow<String>,
    options: kotlinx.coroutines.flow.Flow<List<DropdownChoice<T>>>,
    onSelect: (T) -> Unit
) {
    div("settings-row d-flex flex-wrap align-items-center justify-content-between gap-3 mt-3") {
        div("settings-copy") {
            div("fw-semibold") { +label }
            hint?.let { p("text-body-secondary small mb-0") { +it } }
        }
        bootstrapDropdown(
            runtime = runtime,
            selected = selected,
            selectedLabel = selectedLabel,
            options = options,
            buttonClass = secondaryNavigationButtonClass,
            widthClass = "w-auto",
            onChange = onSelect
        )
    }
}




