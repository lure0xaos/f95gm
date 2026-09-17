package f95gm.client.ui.catalog.toolbar

import dev.fritz2.core.*
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.state.config.ClientConstants
import f95gm.client.state.dropdown.DropdownSearchKeys
import f95gm.client.state.options.dateLimitChoices
import f95gm.client.state.options.dateLimitLabel
import f95gm.client.ui.catalog.filters.filterField
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.domain.api.parseIntOrNull
import f95gm.client.ui.shared.navigation.secondaryNavigationButtonClass
import f95gm.client.state.runtime.ClientCatalogUiPort
import f95gm.domain.filters.F95TagType
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun RenderContext.catalogMatchingSection(runtime: ClientCatalogUiPort) {
    val catalog = runtime.catalogView
    catalogFilterCard(UiMessages.app_timeAndTagMatching()) {
        div("col") {
            filterField(UiMessages.app_dateLimit()) {
                bootstrapDropdown(
                    runtime = runtime,
                    selected = catalog.data.map { it.dateLimit.toString() },
                    selectedLabel = catalog.data.map { dateLimitLabel(it.dateLimit.value) },
                    options = flowOf(dateLimitChoices),
                    buttonClass = "$secondaryNavigationButtonClass text-start",
                    searchKey = ClientConstants.DATE_LIMIT_KEY
                ) { runtime.catalogCommands.selectDateLimit(it.parseIntOrNull() ?: F95Defaults.ANY_DATE) }
            }
        }
        div("col") {
            filterField(UiMessages.app_tagMatching()) {
                bootstrapDropdown(
                    runtime = runtime,
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
                    searchKey = DropdownSearchKeys.TAG_MATCHING
                ) { runtime.catalogCommands.selectTagType(F95TagType.fromWire(it)) }
            }
        }
    }
}
