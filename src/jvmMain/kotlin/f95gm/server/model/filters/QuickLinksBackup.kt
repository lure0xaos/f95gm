package f95gm.server.model.filters

import f95gm.domain.api.SavedFilterRequest
import kotlinx.serialization.Serializable

@Serializable
internal data class QuickLinksBackup(
    val version: Int = 1,
    val quickLinks: List<SavedFilterRequest> = emptyList()
)

