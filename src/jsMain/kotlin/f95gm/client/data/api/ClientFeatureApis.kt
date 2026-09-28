package f95gm.client.data.api

/** Feature-scoped API views prevent actions from depending on an unrelated aggregate surface. */
internal class ClientFeatureApis(private val client: ClientApi) {
    val session: SessionApi = client
    val catalog: CatalogApi = client
    val detail: DetailApi = client
    val marks: MarksApi = client
    val filters: FiltersApi = client
    val health: HealthApi = client
}
