package f95gm.client.state.runtime

import dev.fritz2.core.RootStore
import f95gm.client.data.api.ClientApi
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
import f95gm.client.model.images.DetailGalleryState
import f95gm.client.state.dropdown.DropdownSearchState
import f95gm.client.state.settings.loadAppSettings
import f95gm.client.state.settings.ClientSettingsStore
import f95gm.client.state.catalog.SeenCatalogThreadIds
import f95gm.client.actions.effects.ClientUiEffects
import f95gm.client.actions.effects.RouterClientUiEffects
import f95gm.domain.defaults.F95Defaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/** Feature-owned state ports prevent consumers from depending on the full store aggregate. */
internal data class ClientFeatureStatePorts(
    val catalog: RootStore<CatalogState>,
    val catalogOptions: RootStore<CatalogOptions>,
    val savedFilters: RootStore<SavedFiltersState>,
    val marks: RootStore<MarksState>,
    val detail: RootStore<DetailState>,
    val imageRetry: RootStore<ImageRetryState>
)

/** Runtime-owned state ports keep shell concerns separate from feature stores. */
internal data class ClientShellStatePorts(
    val appSettings: RootStore<AppSettingsState>,
    val session: RootStore<SessionState>,
    val credentials: RootStore<Credentials>,
    val filterPanelOpen: RootStore<Boolean>,
    val savedFilterName: RootStore<String>,
    val jvmConnection: RootStore<JvmConnectionState>,
    val toast: RootStore<ToastState>,
    val detailGallery: RootStore<DetailGalleryState>,
    val dropdownSearches: RootStore<DropdownSearchState>,
    val apiTelemetry: RootStore<ApiTelemetryState>,
    val operationStatuses: RootStore<Map<ClientJobKey, ClientOperationStatus>>,
    val settingsStore: ClientSettingsStore
)

/** API boundary needs only telemetry and connection stores, not the aggregate runtime. */
internal interface ClientApiEventPort {
    val apiTelemetry: RootStore<ApiTelemetryState>
    val jvmConnection: RootStore<JvmConnectionState>
    fun launchEffect(block: suspend () -> Unit): Job
}

/** UI feedback owns only the toast store; navigation remains a separate effect. */
internal interface ClientToastPort {
    val toast: RootStore<ToastState>
}

/** Store projections are kept out of the runtime's API, lifecycle, and command wiring. */
internal interface ClientRuntimeStatePort :
    ClientCatalogStatePort, ClientMarksStatePort, ClientFiltersStatePort, ClientDetailStatePort,
    ClientAuthStatePort, ClientFeedbackStatePort, ClientImageStatePort, ClientGalleryStatePort,
    ClientDropdownStatePort, ClientSettingsStatePort, ClientSettingsViewPort {
    val apiTelemetry: RootStore<ApiTelemetryState>
    val jvmConnection: RootStore<JvmConnectionState>
    override val toast: RootStore<ToastState>
    override val savedFilterName: RootStore<String>
    override val appSettingsView: ClientReadOnlyStore<AppSettingsState>
    override val detailGallery: RootStore<DetailGalleryState>
    override val dropdownSearches: RootStore<DropdownSearchState>
    val catalogView: ClientReadOnlyStore<CatalogState>
    val catalogOptionsView: ClientReadOnlyStore<CatalogOptions>
    val filterPanelOpenView: ClientReadOnlyStore<Boolean>
    val savedFiltersView: ClientReadOnlyStore<SavedFiltersState>
    val savedFilterNameView: ClientReadOnlyStore<String>
    val marksView: ClientReadOnlyStore<MarksState>
    val detailView: ClientReadOnlyStore<DetailState>
    val imageRetryView: ClientReadOnlyStore<ImageRetryState>
    val sessionView: ClientReadOnlyStore<SessionState>
    val credentialsView: ClientReadOnlyStore<Credentials>
    val jvmConnectionView: ClientReadOnlyStore<JvmConnectionState>
    val detailGalleryView: ClientReadOnlyStore<DetailGalleryState>
    val dropdownSearchesView: ClientReadOnlyStore<DropdownSearchState>
    suspend fun updateDetailGallery(update: (DetailGalleryState) -> DetailGalleryState)
    suspend fun updateDropdownSearch(update: (DropdownSearchState) -> DropdownSearchState)
}

private class ClientRuntimeStateAccess(private val state: ClientState) : ClientRuntimeStatePort {
    override val apiTelemetry get() = state.shell.apiTelemetry
    override val jvmConnection get() = state.shell.jvmConnection
    override val catalog get() = state.features.catalog
    override val catalogOptions get() = state.features.catalogOptions
    override val savedFilters get() = state.features.savedFilters
    override val marks get() = state.features.marks
    override val detail get() = state.features.detail
    override val imageRetry get() = state.features.imageRetry
    override val session get() = state.shell.session
    override val credentials get() = state.shell.credentials
    override val filterPanelOpen get() = state.shell.filterPanelOpen
    override val savedFilterName get() = state.shell.savedFilterName
    override val settingsStore get() = state.shell.settingsStore
    override val toast get() = state.shell.toast
    override val detailGallery get() = state.shell.detailGallery
    override val dropdownSearches get() = state.shell.dropdownSearches
    override val appSettingsView get() = state.shell.appSettings.asReadOnly()
    override val catalogView get() = catalog.asReadOnly()
    override val catalogOptionsView get() = catalogOptions.asReadOnly()
    override val filterPanelOpenView get() = filterPanelOpen.asReadOnly()
    override val savedFiltersView get() = savedFilters.asReadOnly()
    override val savedFilterNameView get() = savedFilterName.asReadOnly()
    override val marksView get() = marks.asReadOnly()
    override val detailView get() = detail.asReadOnly()
    override val imageRetryView get() = imageRetry.asReadOnly()
    override val sessionView get() = session.asReadOnly()
    override val credentialsView get() = credentials.asReadOnly()
    override val jvmConnectionView get() = jvmConnection.asReadOnly()
    override val detailGalleryView get() = state.shell.detailGallery.asReadOnly()
    override val dropdownSearchesView get() = state.shell.dropdownSearches.asReadOnly()
    override suspend fun updateDetailGallery(update: (DetailGalleryState) -> DetailGalleryState) {
        state.shell.detailGallery.enqueue(update)
    }
    override suspend fun updateDropdownSearch(update: (DropdownSearchState) -> DropdownSearchState) {
        state.shell.dropdownSearches.enqueue(update)
    }
}

/** Injectable owner for browser state stores and their shared lifecycle job. */
internal class ClientState(
    val controller: ClientController = ClientController(),
    val browser: BrowserEffects = WindowBrowserEffects
) {
    private val storeJob: Job get() = controller.rootStoreJob
    private val storeScope get() = controller.rootStoreScope
    private val authStores = ClientAuthStores(storeJob)
    private val settingsStores = ClientSettingsStores(loadAppSettings(browser.storage), storeJob)
    private val runtimeStores = ClientRuntimeStores(storeJob)
    private val observabilityStores = ClientObservabilityStores(storeJob)
    private val featureStores = ClientFeatureStores(storeJob)
    val settingsStore = ClientSettingsStore(settingsStores.appSettings, browser.storage)

    val features = ClientFeatureStatePorts(
        catalog = featureStores.catalog,
        catalogOptions = featureStores.catalogOptions,
        savedFilters = featureStores.savedFilters,
        marks = featureStores.marks,
        detail = featureStores.detail,
        imageRetry = featureStores.imageRetry
    )
    val shell = ClientShellStatePorts(
        appSettings = settingsStores.appSettings,
        session = authStores.session,
        credentials = authStores.credentials,
        filterPanelOpen = runtimeStores.filterPanelOpen,
        savedFilterName = featureStores.savedFilterName,
        jvmConnection = runtimeStores.jvmConnection,
        toast = runtimeStores.toast,
        detailGallery = runtimeStores.detailGallery,
        dropdownSearches = featureStores.dropdownSearches,
        apiTelemetry = observabilityStores.apiTelemetry,
        operationStatuses = observabilityStores.operationStatuses,
        settingsStore = settingsStore
    )

    init {
        controller.setOperationStatusSink(ClientOperationStatusSink { key, status ->
            storeScope.launch {
                observabilityStores.operationStatuses.enqueue { current -> current + (key to status) }
            }
        })
    }
}

internal class ClientRuntime(
    val state: ClientState,
    private val overrides: ClientRuntimeOverrides = ClientRuntimeOverrides(),
    private val dependencies: ClientRuntimeDependencies
) : ClientUiPort, ClientActionPort, ClientApiEventPort, ClientToastPort,
    ClientRuntimeStatePort by ClientRuntimeStateAccess(state) {
    override val loginCommands: ClientLoginCommands = ClientLoginCommandsAdapter(this)
    override val sessionCommands: ClientSessionCommands = ClientSessionCommandsAdapter(this)
    override val catalogCommands: ClientCatalogCommands = ClientCatalogCommandsAdapter(this)
    override val marksCommands: ClientMarksCommands = ClientMarksCommandsAdapter(this)
    override val filtersCommands: ClientFiltersCommands = ClientFiltersCommandsAdapter(this)
    override val detailCommands: ClientDetailCommands = ClientDetailCommandsAdapter(this)
    override val imagesCommands: ClientImagesCommands = ClientImagesCommandsAdapter(this)
    override val settingsCommands: ClientSettingsCommands = ClientSettingsCommandsAdapter(this)
    override val galleryCommands: ClientGalleryCommands = ClientGalleryCommandsAdapter(this)
    override val dropdownCommands: ClientDropdownCommands = ClientDropdownCommandsAdapter(this)
    private val lifecycle = ClientRuntimeLifecycle()
    private val apiComposition = ClientApiComposition(this, overrides.apiClient, dependencies, overrides.telemetrySink)
    override val browser: BrowserEffects get() = state.browser
    override val browserTabId: BrowserTabId = BrowserTabId(browser.storage)
    override val seenCatalogThreadIds: SeenCatalogThreadIds = overrides.seenCatalogThreadIds ?: SeenCatalogThreadIds(browser.storage)
    private val controller: ClientController get() = state.controller
    override val requests: ClientRequestTrackingPort get() = controller
    override val catalogLoading: ClientCatalogLoadingPort = ClientCatalogLoadingAdapter(controller)
    override val marksLoading: ClientFeatureLoadingPort = ClientFeatureLoadingAdapter(controller, ClientLoadState.MARKS)
    override val filtersLoading: ClientFiltersLoadingPort = ClientFiltersLoadingAdapter(controller)
    override val detailLoading: ClientDetailLoadingPort = ClientDetailLoadingAdapter(controller)
    override val sessionLifecycle: ClientSessionLifecyclePort = ClientSessionLifecyclePort(controller::resetSessionLifecycle)
    override val healthProbes: ClientHealthProbePort = ClientHealthProbePort { controller.healthProbeNumber++ }
    val scope get() = controller.scope
    val apiEvents get() = apiComposition.apiEvents
    override val clock: ClientClock get() = dependencies.clock
    val apiExecutor get() = apiComposition.apiExecutor
    val apiClient: ClientApi get() = apiComposition.apiClient
    val features = ClientFeaturePorts(
        session = apiComposition.session,
        catalog = apiComposition.catalog,
        detail = apiComposition.detail,
        marks = apiComposition.marks,
        filters = apiComposition.filters,
        health = apiComposition.health
    )
    override val sessionApi: SessionApi get() = features.session
    override val catalogApi: CatalogApi get() = features.catalog
    override val detailApi: DetailApi get() = features.detail
    override val marksApi: MarksApi get() = features.marks
    override val filtersApi: FiltersApi get() = features.filters
    override val healthApi: HealthApi get() = features.health
    override val uiEffects: ClientUiEffects = overrides.uiEffects ?: RouterClientUiEffects(this)
    override fun launchOperation(key: ClientJobKey, block: suspend () -> Unit): Job = controller.launchOperation(key, block)
    override fun launchEffect(block: suspend () -> Unit): Job = scope.launch { block() }
    fun start() {
        lifecycle.start()
        check(controller.rootStoreJob.isActive) { "Client root stores are closed; create a new runtime" }
        if (!scope.isActive) controller.replaceScope()
    }
    fun close() {
        if (!lifecycle.close()) return
        try {
            controller.close()
        } finally {
            if (dependencies.httpClientOwnership == HttpClientOwnership.Owned) dependencies.httpClient.close()
        }
    }
}
