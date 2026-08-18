package f95gm.server.model.detail

import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
internal data class DetailMeta(val label: F95OptionName, val value: F95OptionName)
