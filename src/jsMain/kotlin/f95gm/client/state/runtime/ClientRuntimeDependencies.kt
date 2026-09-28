package f95gm.client.state.runtime

import io.ktor.client.HttpClient
import f95gm.client.state.catalog.SeenCatalogThreadIds
import f95gm.client.data.api.ApiTelemetrySink
import f95gm.client.data.api.CatalogApi
import f95gm.client.data.api.DetailApi
import f95gm.client.data.api.FiltersApi
import f95gm.client.data.api.HealthApi
import f95gm.client.data.api.MarksApi
import f95gm.client.data.api.SessionApi

/** Feature-scoped client ports used by actions and UI composition. */
internal data class ClientFeaturePorts(
    val session: SessionApi,
    val catalog: CatalogApi,
    val detail: DetailApi,
    val marks: MarksApi,
    val filters: FiltersApi,
    val health: HealthApi
)

internal sealed interface HttpClientOwnership {
    data object Owned : HttpClientOwnership
    data object Borrowed : HttpClientOwnership
}

internal data class ClientRuntimeDependencies(
    val httpClient: HttpClient,
    val httpClientOwnership: HttpClientOwnership,
    val clock: ClientClock
) {
    companion object {
        fun production() = ClientRuntimeDependencies(
            httpClient = createHttpClient(),
            httpClientOwnership = HttpClientOwnership.Owned,
            clock = SystemClientClock
        )
    }
}

/** Explicit test/integration substitutions; production wiring stays non-null and deterministic. */
internal data class ClientRuntimeOverrides(
    val apiClient: f95gm.client.data.api.ClientApi? = null,
    val uiEffects: f95gm.client.actions.effects.ClientUiEffects? = null,
    val seenCatalogThreadIds: SeenCatalogThreadIds? = null,
    val telemetrySink: ApiTelemetrySink? = null
)
