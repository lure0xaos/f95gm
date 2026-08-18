package f95gm.client.actions.auth

import f95gm.client.data.api.postJson
import f95gm.client.model.auth.SessionState
import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.detail.DetailState
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.model.marks.MarksState
import f95gm.client.routing.navigation.loginRoute
import f95gm.client.routing.navigation.router
import f95gm.client.state.catalog.catalog
import f95gm.client.state.catalog.catalogOptions
import f95gm.client.state.catalog.loadedOptionsCategory
import f95gm.client.state.detail.detail
import f95gm.client.state.detail.loadedDetailId
import f95gm.client.state.filters.*
import f95gm.client.state.marks.loadedMarks
import f95gm.client.state.marks.marks
import f95gm.client.state.runtime.clientJson
import f95gm.client.state.runtime.scope
import f95gm.client.state.session.loaded
import f95gm.client.state.session.session
import f95gm.domain.api.F95Api
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

internal fun signOut() {
    scope.launch {
        runCatching { postJson(F95Api.LOGOUT, clientJson.encodeToString(JsonObject(emptyMap()))) }
        session.enqueue { SessionState() }
        catalog.enqueue { CatalogState() }
        catalogOptions.enqueue { CatalogOptions() }
        savedFilters.enqueue { SavedFiltersState() }
        savedFilterName.enqueue { "" }
        detail.enqueue { DetailState() }
        marks.enqueue { MarksState() }
        loaded = false
        loadedDetailId = null
        loadedOptionsCategory = null
        loadedMarks = false
        loadedSavedFilters = false
        savedFiltersLoadJob = null
        appliedFilterId = null
        router.navTo(loginRoute)
    }
}
