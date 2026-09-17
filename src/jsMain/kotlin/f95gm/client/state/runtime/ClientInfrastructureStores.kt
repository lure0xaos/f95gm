package f95gm.client.state.runtime

import dev.fritz2.core.RootStore
import f95gm.client.model.auth.Credentials
import f95gm.client.model.auth.SessionState
import f95gm.client.model.feedback.ToastState
import f95gm.client.model.settings.AppSettingsState
import f95gm.client.model.images.DetailGalleryState
import kotlinx.coroutines.Job

internal class ClientAuthStores(storeJob: Job) {
    val session = RootStore(SessionState(async = AsyncState.Loading), storeJob)
    val credentials = RootStore(Credentials("", ""), storeJob)
}

internal class ClientSettingsStores(initial: AppSettingsState, storeJob: Job) {
    val appSettings = RootStore(initial, storeJob)
}

internal class ClientRuntimeStores(storeJob: Job) {
    val filterPanelOpen = RootStore(false, storeJob)
    val jvmConnection = RootStore(JvmConnectionState(), storeJob)
    val toast = RootStore(ToastState(), storeJob)
    val detailGallery = RootStore(DetailGalleryState(), storeJob)
}

internal class ClientObservabilityStores(storeJob: Job) {
    val apiTelemetry = RootStore(ApiTelemetryState(), storeJob)
    val operationStatuses = RootStore<Map<ClientJobKey, ClientOperationStatus>>(emptyMap(), storeJob)
}
