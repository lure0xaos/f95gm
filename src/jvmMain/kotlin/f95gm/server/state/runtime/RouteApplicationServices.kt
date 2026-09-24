package f95gm.server.state.runtime

import f95gm.server.http.request.MutationService
import f95gm.server.persistence.MarkRepository
import f95gm.server.persistence.SavedFilterRepository

internal interface RouteApplicationServices {
    val marks: MarkRepository
    val savedFilters: SavedFilterRepository
    val mutations: MutationService
}
