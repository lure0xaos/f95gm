package f95gm.server.persistence.filters

import f95gm.server.model.filters.SavedFilter
import f95gm.server.state.runtime.upstreamJson

internal fun savedFiltersJson(filters: List<SavedFilter>): String = upstreamJson.encodeToString(filters)
