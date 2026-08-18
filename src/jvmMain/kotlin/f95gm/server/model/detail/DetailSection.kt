package f95gm.server.model.detail

import f95gm.domain.values.F95Html
import f95gm.domain.values.F95OptionName
import kotlinx.serialization.Serializable

@Serializable
internal data class DetailSection(val title: F95OptionName, val html: F95Html)
