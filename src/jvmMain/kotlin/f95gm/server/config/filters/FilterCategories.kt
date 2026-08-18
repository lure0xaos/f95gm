package f95gm.server.config.filters

import f95gm.domain.filters.F95Category

internal val filterCategories = F95Category.entries.map { it.wireValue }.toSet()
