package f95gm.client.actions.catalog.filters.selection

import f95gm.client.state.catalog.catalog
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.runtime.scope
import f95gm.domain.values.F95OptionId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun addSelectedOption(value: String, excluded: Boolean, prefix: Boolean, groupId: F95OptionId? = null) {
    if (value.isBlank()) return
    scope.launch {
        val optionsState = catalogOptions.data.first()
        val current = catalog.data.first()
        val next = if (prefix) {
            val options = current.let {
                optionsState.prefixGroups.firstOrNull { it.id == groupId }?.options ?: optionsState.prefixes
            }
            val selected = options.firstOrNull { it.id.value == value } ?: return@launch
            val existing = if (excluded) current.excludedPrefixes else current.selectedPrefixes
            val toggled = if (existing.any { it.id.value == value }) {
                existing.filterNot { it.id.value == value }
            } else existing + selected
            if (excluded) current.copy(
                excludedPrefixes = toggled,
                excludedPrefixPickers = current.excludedPrefixPickers + (requireNotNull(groupId) to "")
            ) else current.copy(
                selectedPrefixes = toggled,
                prefixPickers = current.prefixPickers + (requireNotNull(groupId) to "")
            )
        } else {
            val selected = optionsState.tags.firstOrNull { it.id.value == value } ?: return@launch
            val existing = if (excluded) current.excludedTags else current.selectedTags
            val toggled = if (existing.any { it.id.value == value }) {
                existing.filterNot { it.id.value == value }
            } else existing + selected
            if (excluded) current.copy(excludedTags = toggled, excludedTagPicker = "")
            else current.copy(selectedTags = toggled, tagPicker = "")
        }
        catalog.enqueue { next }
    }
}
