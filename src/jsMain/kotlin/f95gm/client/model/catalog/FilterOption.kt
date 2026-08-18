package f95gm.client.model.catalog

import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName

internal data class FilterOption(val id: F95OptionId, val name: F95OptionName) : FilterChoice {
    override val value: String get() = id.value
    override val label: String get() = name.value
}
