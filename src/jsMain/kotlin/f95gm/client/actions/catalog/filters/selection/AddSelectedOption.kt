package f95gm.client.actions.catalog.filters.selection

import f95gm.client.model.catalog.FilterOptionKind
import f95gm.client.model.catalog.FilterSelection
import f95gm.client.state.runtime.ClientCatalogActionsPort
import f95gm.domain.values.F95OptionId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun addSelectedOption(
    runtime: ClientCatalogActionsPort,
    value: String,
    selection: FilterSelection,
    kind: FilterOptionKind,
    groupId: F95OptionId? = null
) {
    if (value.isBlank()) return
    runtime.launchEffect {
        val optionsState = runtime.catalogOptions.data.first()
        val current = runtime.catalog.data.first()
        val next = if (kind == FilterOptionKind.PREFIX) {
            val options = current.let {
                optionsState.prefixGroups.firstOrNull { it.id == groupId }?.options ?: optionsState.prefixes
            }
            val selected = options.firstOrNull { it.id.value == value } ?: return@launchEffect
            val existing =
                if (selection == FilterSelection.EXCLUDED) current.excludedPrefixes else current.selectedPrefixes
            val toggled = if (existing.any { it.id.value == value }) {
                existing.filterNot { it.id.value == value }
            } else existing + selected
            if (selection == FilterSelection.EXCLUDED) current.copy(
                excludedPrefixes = toggled,
                excludedPrefixPickers = current.excludedPrefixPickers + (requireNotNull(groupId) to "")
            ) else current.copy(
                selectedPrefixes = toggled,
                prefixPickers = current.prefixPickers + (requireNotNull(groupId) to "")
            )
        } else {
            val selected = optionsState.tags.firstOrNull { it.id.value == value } ?: return@launchEffect
            val existing = if (selection == FilterSelection.EXCLUDED) current.excludedTags else current.selectedTags
            val toggled = if (existing.any { it.id.value == value }) {
                existing.filterNot { it.id.value == value }
            } else existing + selected
            if (selection == FilterSelection.EXCLUDED) current.copy(excludedTags = toggled, excludedTagPicker = "")
            else current.copy(selectedTags = toggled, tagPicker = "")
        }
        runtime.catalog.enqueue { next }
    }
}
