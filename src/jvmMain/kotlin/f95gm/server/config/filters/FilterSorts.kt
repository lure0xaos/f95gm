package f95gm.server.config.filters

import f95gm.domain.filters.F95Sort

internal val filterSorts = F95Sort.entries.map { it.wireValue }.toSet()
