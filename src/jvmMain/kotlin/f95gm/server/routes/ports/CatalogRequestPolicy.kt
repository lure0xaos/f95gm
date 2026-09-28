package f95gm.server.routes.ports

import f95gm.server.config.CatalogPageLimit
import f95gm.server.config.CatalogTagLimit
import f95gm.server.config.CatalogTextLength

internal data class CatalogRequestPolicy(
    val defaultRows: Int,
    val allowedRows: Set<Int>,
    val maxPage: CatalogPageLimit,
    val maxTags: CatalogTagLimit,
    val maxTextLength: CatalogTextLength
) {
    internal constructor(
        defaultRows: Int,
        allowedRows: Set<Int>,
        maxPage: Int,
        maxTags: Int,
        maxTextLength: Int
    ) : this(
        defaultRows,
        allowedRows,
        CatalogPageLimit(maxPage),
        CatalogTagLimit(maxTags),
        CatalogTextLength(maxTextLength)
    )
}
