package f95gm.server.model.filters

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedFilterOption(
    val id: F95OptionId = F95OptionId(F95Defaults.EMPTY),
    val name: F95OptionName = F95OptionName(F95Defaults.EMPTY)
)
