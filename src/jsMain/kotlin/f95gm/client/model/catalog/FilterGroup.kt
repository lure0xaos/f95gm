package f95gm.client.model.catalog

import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName

internal data class FilterGroup(
    val id: F95OptionId,
    val name: F95OptionName,
    val options: List<PrefixOption>
)
