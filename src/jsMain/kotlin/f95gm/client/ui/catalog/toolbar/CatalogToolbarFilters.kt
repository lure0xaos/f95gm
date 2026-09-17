package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.dropdown.DropdownSearchKeys
import f95gm.client.state.options.dateLimitChoices
import f95gm.client.state.options.dateLimitLabel
import f95gm.client.ui.catalog.filters.filterField
import f95gm.client.ui.catalog.filters.filterPicker
import f95gm.client.ui.navigation.NavbarPage
import f95gm.client.ui.navigation.responsiveNavbar
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.navigationButtonClass
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.ui.shared.textFieldClass
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95TagType
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogFilterPanel(runtime: ClientCatalogUiPort) =
    renderCatalogFilterSections(runtime)
