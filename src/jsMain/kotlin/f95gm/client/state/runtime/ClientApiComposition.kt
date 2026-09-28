package f95gm.client.state.runtime

import f95gm.client.data.api.ApiClientEvents
import f95gm.client.data.api.ApiRequestExecutor
import f95gm.client.data.api.ApiTransport
import f95gm.client.data.api.ClientApi
import f95gm.client.data.api.ClientFeatureApis
import f95gm.client.data.api.RuntimeApiClient
import f95gm.client.data.api.RuntimeApiClientEvents
import f95gm.client.data.api.RuntimeApiTelemetrySink
import f95gm.client.data.api.CatalogApi
import f95gm.client.data.api.DetailApi
import f95gm.client.data.api.FiltersApi
import f95gm.client.data.api.HealthApi
import f95gm.client.data.api.MarksApi
import f95gm.client.data.api.SessionApi

/** Owns API transport, telemetry, and feature-client assembly for one runtime instance. */
internal class ClientApiComposition(
    owner: ClientApiEventPort,
    private val suppliedApiClient: ClientApi?,
    private val dependencies: ClientRuntimeDependencies,
    private val telemetrySink: f95gm.client.data.api.ApiTelemetrySink?
) {
    val apiEvents: ApiClientEvents = RuntimeApiClientEvents(owner, telemetrySink ?: RuntimeApiTelemetrySink(owner))
    val apiExecutor = ApiRequestExecutor(
        ApiTransport(dependencies.httpClient, dependencies.clock),
        apiEvents,
        clock = dependencies.clock
    )
    val apiClient: ClientApi = suppliedApiClient ?: RuntimeApiClient(apiExecutor)
    private val featureApis = ClientFeatureApis(apiClient)
    val session: SessionApi get() = featureApis.session
    val catalog: CatalogApi get() = featureApis.catalog
    val detail: DetailApi get() = featureApis.detail
    val marks: MarksApi get() = featureApis.marks
    val filters: FiltersApi get() = featureApis.filters
    val health: HealthApi get() = featureApis.health
}
