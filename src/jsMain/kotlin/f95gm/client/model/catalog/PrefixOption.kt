package f95gm.client.model.catalog

import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95PrefixId

internal data class PrefixOption(val id: F95PrefixId, val name: F95OptionName) : FilterChoice {
    override val value: String get() = id.value
    override val label: String get() = name.value
}
