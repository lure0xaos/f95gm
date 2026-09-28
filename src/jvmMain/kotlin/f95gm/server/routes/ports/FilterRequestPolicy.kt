package f95gm.server.routes.ports

import f95gm.server.config.FilterIdLength
import f95gm.server.config.ImportedQuickLinkLimit

internal data class FilterRequestPolicy(
    val maxImportedQuickLinks: ImportedQuickLinkLimit,
    val maxFilterIdLength: FilterIdLength
)
