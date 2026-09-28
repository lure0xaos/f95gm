package f95gm.domain.api

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
data class FilterOptionRequest(
    val id: F95OptionId = F95OptionId(F95Defaults.EMPTY),
    val name: F95OptionName = F95OptionName(F95Defaults.EMPTY)
)
