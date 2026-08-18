package f95gm.client.model.catalog

import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95TagId

internal data class TagOption(val id: F95TagId, val name: F95OptionName) : FilterChoice {
    override val value: String get() = id.value
    override val label: String get() = name.value
}
