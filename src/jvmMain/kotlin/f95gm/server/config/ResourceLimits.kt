package f95gm.server.config

import f95gm.server.config.constants.FilterLimits

internal data class ResourceLimits(
    val maxSessions: SessionCapacity = SessionCapacity(SessionLimits.MAX_SESSIONS),
    val maxRequestBodyBytes: Bytes = Bytes(RequestLimits.MAX_BODY_BYTES),
    val maxMetricEndpoints: Int = RequestLimits.MAX_METRIC_ENDPOINTS,
    val maxUpstreamMetricEndpoints: Int = UpstreamLimits.MAX_METRIC_ENDPOINTS,
    val maxUpstreamConcurrentRequests: UpstreamConcurrencyLimit = UpstreamConcurrencyLimit(UpstreamLimits.MAX_CONCURRENT_REQUESTS),
    val maxMediaResponseBytes: Bytes = Bytes(RequestLimits.MAX_MEDIA_RESPONSE_BYTES),
    val mediaDownloadBufferBytes: MediaDownloadBufferBytes = MediaDownloadBufferBytes(MediaLimits.DOWNLOAD_BUFFER_BYTES),
    val maxCatalogPage: CatalogPageLimit = CatalogPageLimit(CatalogLimits.MAX_PAGE),
    val maxCatalogTags: CatalogTagLimit = CatalogTagLimit(CatalogLimits.MAX_TAGS),
    val maxCatalogTextLength: CatalogTextLength = CatalogTextLength(CatalogLimits.MAX_TEXT_LENGTH),
    val maxImportedQuickLinks: ImportedQuickLinkLimit = ImportedQuickLinkLimit(FilterLimits.MAX_QUICK_LINKS),
    val maxFilterIdLength: FilterIdLength = FilterIdLength(FilterLimits.FILTER_ID_LENGTH)
) {
    companion object {
        fun from(properties: ConfigProperties) = ResourceLimits(
            maxSessions = SessionCapacity(properties.positiveInt(Keys.MAX_SESSIONS)),
            maxRequestBodyBytes = properties.positiveBytes(Keys.MAX_REQUEST_BODY),
            maxMetricEndpoints = properties.positiveInt(Keys.MAX_METRIC_ENDPOINTS),
            maxUpstreamMetricEndpoints = properties.positiveInt(Keys.MAX_UPSTREAM_METRIC_ENDPOINTS),
            maxUpstreamConcurrentRequests = UpstreamConcurrencyLimit(properties.positiveInt(Keys.MAX_UPSTREAM_CONCURRENT_REQUESTS)),
            maxMediaResponseBytes = properties.positiveBytes(Keys.MAX_MEDIA_RESPONSE_BYTES),
            mediaDownloadBufferBytes = MediaDownloadBufferBytes(properties.positiveInt(Keys.MEDIA_DOWNLOAD_BUFFER_BYTES)),
            maxCatalogPage = CatalogPageLimit(properties.positiveInt(Keys.MAX_CATALOG_PAGE)),
            maxCatalogTags = CatalogTagLimit(properties.positiveInt(Keys.MAX_CATALOG_TAGS)),
            maxCatalogTextLength = CatalogTextLength(properties.positiveInt(Keys.MAX_CATALOG_TEXT_LENGTH)),
            maxImportedQuickLinks = ImportedQuickLinkLimit(properties.positiveInt(Keys.MAX_IMPORTED_QUICK_LINKS)),
            maxFilterIdLength = FilterIdLength(properties.positiveInt(Keys.MAX_FILTER_ID_LENGTH))
        )
    }

    fun validationIssues(timeouts: TimeoutConfig): List<ConfigurationIssue> = configurationIssues {
        positive(maxRequestBodyBytes.value, "Maximum request body size must be positive")
        positive(maxMetricEndpoints.toLong(), "Maximum metric endpoint count must be positive")
        positive(maxUpstreamMetricEndpoints.toLong(), "Maximum upstream metric endpoint count must be positive")
        positive(maxMediaResponseBytes.value, "Maximum media response size must be positive")
        positive(timeouts.sessionIdleMillis.value, "Session idle timeout must be positive")
    }

    fun validate(timeouts: TimeoutConfig) = validationIssues(timeouts).throwValidation("Resource limits")
}
