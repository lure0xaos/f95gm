package f95gm.client.model.runtime

internal value class ApiEndpointId(val value: String) {
    init { require(value.isNotBlank()) }
}
