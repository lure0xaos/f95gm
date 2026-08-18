package f95gm.client.model.filters

import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedFilterOptionBody(val id: F95OptionId, val name: F95OptionName)
