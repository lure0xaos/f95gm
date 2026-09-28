package f95gm.client.state.runtime

import dev.fritz2.core.RootStore
import f95gm.client.model.images.DetailGalleryState
import f95gm.client.data.api.SessionApi
import f95gm.client.data.api.CatalogApi
import f95gm.client.data.api.DetailApi
import f95gm.client.data.api.MarksApi
import f95gm.client.data.api.FiltersApi
import f95gm.client.data.api.HealthApi
import f95gm.client.model.catalog.CatalogOptions
import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.detail.DetailState
import f95gm.client.model.filters.SavedFiltersState
import f95gm.client.model.images.ImageRetryState
import f95gm.client.model.marks.MarksState
import f95gm.client.model.auth.Credentials
import f95gm.client.model.auth.SessionState
import f95gm.client.model.feedback.ToastState
import f95gm.client.model.settings.AppSettingsState
import f95gm.client.state.dropdown.DropdownSearchState
import f95gm.client.state.settings.ClientSettingsStore
import f95gm.client.state.catalog.SeenCatalogThreadIds
import f95gm.client.actions.effects.ClientUiEffects
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import dev.fritz2.core.Store

/** UI projection of a store that exposes observation without exposing its update channel. */
internal class ClientReadOnlyStore<T>(private val store: Store<T>) {
    val data: Flow<T> get() = store.data
    val current: T get() = store.current
}

internal fun <T> RootStore<T>.asReadOnly() = ClientReadOnlyStore(this)

/** Shared operation lifecycle; feature state is exposed only by the corresponding capability. */
internal interface ClientOperationPort {
    val requests: ClientRequestTrackingPort
    val clock: ClientClock
    val uiEffects: ClientUiEffects
    fun launchOperation(key: ClientJobKey, block: suspend () -> Unit): Job
    fun launchEffect(block: suspend () -> Unit): Job
}

internal interface ClientCatalogStatePort {
    val catalog: RootStore<CatalogState>
    val catalogOptions: RootStore<CatalogOptions>
    val filterPanelOpen: RootStore<Boolean>
}

internal interface ClientMarksStatePort {
    val marks: RootStore<MarksState>
}

internal interface ClientFiltersStatePort {
    val savedFilters: RootStore<SavedFiltersState>
    val savedFilterName: RootStore<String>
}

internal interface ClientDetailStatePort {
    val detail: RootStore<DetailState>
}

internal interface ClientSettingsViewPort {
    val appSettingsView: ClientReadOnlyStore<AppSettingsState>
}

internal interface ClientSettingsStatePort {
    val settingsStore: ClientSettingsStore
}

internal interface ClientAuthStatePort {
    val session: RootStore<SessionState>
    val credentials: RootStore<Credentials>
}

internal interface ClientFeedbackStatePort {
    val toast: RootStore<ToastState>
}

internal interface ClientImageStatePort {
    val imageRetry: RootStore<ImageRetryState>
}

internal interface ClientGalleryStatePort {
    val detailGallery: RootStore<DetailGalleryState>
}

internal interface ClientDropdownStatePort {
    val dropdownSearches: RootStore<DropdownSearchState>
}

internal interface ClientGalleryActionsPort : ClientOperationPort {
    suspend fun updateDetailGallery(update: (DetailGalleryState) -> DetailGalleryState)
}

internal interface ClientDropdownActionsPort : ClientOperationPort {
    suspend fun updateDropdownSearch(update: (DropdownSearchState) -> DropdownSearchState)
}

internal interface ClientFeedbackActionsPort : ClientOperationPort, ClientFeedbackStatePort

internal interface ClientMarksActionsPort :
    ClientFeedbackActionsPort, ClientMarksStatePort, ClientSettingsViewPort {
    val marksApi: MarksApi
    val marksLoading: ClientFeatureLoadingPort
    val seenCatalogThreadIds: SeenCatalogThreadIds
    val session: RootStore<SessionState>
}

internal interface ClientCatalogActionsPort : ClientFeedbackActionsPort, ClientCatalogStatePort, ClientSettingsViewPort {
    val seenCatalogThreadIds: SeenCatalogThreadIds
    val catalogApi: CatalogApi
    val catalogLoading: ClientCatalogLoadingPort
}

/** Catalog bootstrap also loads the marks displayed on catalog cards. */
internal interface ClientCatalogBootstrapActionsPort : ClientCatalogActionsPort, ClientMarksActionsPort

internal interface ClientFiltersActionsPort : ClientCatalogActionsPort, ClientFiltersStatePort {
    val filtersApi: FiltersApi
    val filtersLoading: ClientFiltersLoadingPort
    val browser: BrowserEffects
}

internal interface ClientDetailActionsPort : ClientFeedbackActionsPort, ClientDetailStatePort {
    val detailApi: DetailApi
    val detailLoading: ClientDetailLoadingPort
}

/** Sign-out resets each feature through its store capability. */
internal interface ClientAuthActionsPort :
    ClientMarksActionsPort, ClientCatalogStatePort, ClientAuthStatePort, ClientFiltersStatePort, ClientDetailStatePort {
    val sessionApi: SessionApi
    val sessionLifecycle: ClientSessionLifecyclePort
}

internal interface ClientSettingsActionsPort : ClientCatalogActionsPort, ClientMarksStatePort, ClientSettingsStatePort

internal interface ClientConnectionActionsPort : ClientOperationPort, ClientApiEventPort, ClientSettingsViewPort {
    val browser: BrowserEffects
    val browserTabId: BrowserTabId
    val healthApi: HealthApi
    val healthProbes: ClientHealthProbePort
}

internal interface ClientImageActionsPort : ClientOperationPort, ClientImageStatePort

/** Mutable capabilities are combined only in the runtime composition owner. */
internal interface ClientActionPort :
    ClientCatalogBootstrapActionsPort, ClientFiltersActionsPort, ClientAuthActionsPort,
    ClientSettingsActionsPort, ClientDetailActionsPort, ClientConnectionActionsPort,
    ClientImageActionsPort, ClientGalleryActionsPort, ClientDropdownActionsPort

internal interface ClientDropdownUiPort {
    val dropdownCommands: ClientDropdownCommands
    val dropdownSearchesView: ClientReadOnlyStore<DropdownSearchState>
}
internal interface ClientGalleryUiPort {
    val galleryCommands: ClientGalleryCommands
    val detailGalleryView: ClientReadOnlyStore<DetailGalleryState>
}
internal interface ClientCardUiPort : ClientSettingsViewPort {
    val imagesCommands: ClientImagesCommands
    val imageRetryView: ClientReadOnlyStore<ImageRetryState>
}
internal interface ClientNavbarReloadPort {
    val catalogCommands: ClientCatalogCommands
    val marksCommands: ClientMarksCommands
    val catalogView: ClientReadOnlyStore<CatalogState>
    val marksView: ClientReadOnlyStore<MarksState>
}
internal interface ClientSavedFiltersUiPort {
    val savedFiltersView: ClientReadOnlyStore<SavedFiltersState>
}
internal interface ClientCatalogUiPort : ClientNavbarReloadPort, ClientDropdownUiPort, ClientCardUiPort {
    val filtersCommands: ClientFiltersCommands
    val catalogOptionsView: ClientReadOnlyStore<CatalogOptions>
    val filterPanelOpenView: ClientReadOnlyStore<Boolean>
    val savedFiltersView: ClientReadOnlyStore<SavedFiltersState>
    val savedFilterNameView: ClientReadOnlyStore<String>
}
internal interface ClientMarkControlsUiPort : ClientDropdownUiPort {
    val marksCommands: ClientMarksCommands
    val marksView: ClientReadOnlyStore<MarksState>
}
internal interface ClientMarksUiPort : ClientMarkControlsUiPort, ClientCardUiPort {
    val catalogCommands: ClientCatalogCommands
    val catalogOptionsView: ClientReadOnlyStore<CatalogOptions>
}
internal interface ClientDetailUiPort : ClientMarkControlsUiPort, ClientGalleryUiPort {
    val detailCommands: ClientDetailCommands
    val detailView: ClientReadOnlyStore<DetailState>
}
internal interface ClientNavigationUiPort : ClientSavedFiltersUiPort, ClientNavbarReloadPort {
    val sessionCommands: ClientSessionCommands
    val filtersCommands: ClientFiltersCommands
    val jvmConnectionView: ClientReadOnlyStore<JvmConnectionState>
    val sessionView: ClientReadOnlyStore<SessionState>
    val savedFilterNameView: ClientReadOnlyStore<String>
}
internal interface ClientSettingsUiPort : ClientSettingsViewPort, ClientDropdownUiPort {
    val settingsCommands: ClientSettingsCommands
    val filtersCommands: ClientFiltersCommands
}
internal interface ClientLoginUiPort {
    val loginCommands: ClientLoginCommands
    val credentialsView: ClientReadOnlyStore<Credentials>
    val sessionView: ClientReadOnlyStore<SessionState>
}

/** Only page composition combines navigation and the capabilities of its screen. */
internal interface ClientCatalogPagePort : ClientCatalogUiPort, ClientNavigationUiPort
internal interface ClientMarksPagePort : ClientMarksUiPort, ClientNavigationUiPort
internal interface ClientDetailPagePort : ClientDetailUiPort, ClientNavigationUiPort
internal interface ClientSettingsPagePort : ClientSettingsUiPort, ClientNavigationUiPort

/** Bootstrap capability aggregate; rendering leaves receive feature ports. */
internal interface ClientUiPort :
    ClientCatalogPagePort, ClientMarksPagePort, ClientDetailPagePort, ClientSettingsPagePort,
    ClientLoginUiPort
