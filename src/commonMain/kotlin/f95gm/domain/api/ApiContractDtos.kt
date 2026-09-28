package f95gm.domain.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import f95gm.domain.values.F95EpochMillis

/** Serialization models generated from the stable API schema vocabulary. */
@Serializable data class ApiErrorDetailsContract(
    val field: String? = null,
    val reason: String? = null,
    val status: Int? = null,
    val expectedUpdatedAt: F95EpochMillis? = null,
    val actualUpdatedAt: F95EpochMillis? = null,
    @Transient val extensions: Map<String, String> = emptyMap()
) {
    fun toWireMap(): Map<String, String> = buildMap {
        putAll(extensions)
        field?.let { put(ApiErrorDetailKeys.FIELD, it) }
        reason?.let { put(ApiErrorDetailKeys.REASON, it) }
        status?.let { put(ApiErrorDetailKeys.STATUS, it.toString()) }
        expectedUpdatedAt?.let { put(ApiErrorDetailKeys.EXPECTED_UPDATED_AT, it.value.toString()) }
        actualUpdatedAt?.let { put(ApiErrorDetailKeys.ACTUAL_UPDATED_AT, it.value.toString()) }
    }

    fun asMap(): Map<String, String> = toWireMap()

    companion object {
        fun fromMap(values: Map<String, String>) = ApiErrorDetailsContract(
            field = values[ApiErrorDetailKeys.FIELD],
            reason = values[ApiErrorDetailKeys.REASON],
            status = values[ApiErrorDetailKeys.STATUS]?.parseIntOrNull(),
            expectedUpdatedAt = values[ApiErrorDetailKeys.EXPECTED_UPDATED_AT]?.parseNonNegativeLong()?.let(::F95EpochMillis),
            actualUpdatedAt = values[ApiErrorDetailKeys.ACTUAL_UPDATED_AT]?.parseNonNegativeLong()?.let(::F95EpochMillis),
            extensions = values.filterKeys { it !in knownKeys }
        )

        private val knownKeys = setOf(
            ApiErrorDetailKeys.FIELD, ApiErrorDetailKeys.REASON, ApiErrorDetailKeys.STATUS,
            ApiErrorDetailKeys.EXPECTED_UPDATED_AT, ApiErrorDetailKeys.ACTUAL_UPDATED_AT
        )
    }
}

private object ApiErrorDetailKeys {
    const val FIELD = "field"
    const val REASON = "reason"
    const val STATUS = "status"
    const val EXPECTED_UPDATED_AT = "expectedUpdatedAt"
    const val ACTUAL_UPDATED_AT = "actualUpdatedAt"
}

@Serializable data class ApiErrorContract(
    val message: String = "", val code: String = "", val messageKey: String? = null,
    val details: Map<String, String> = emptyMap()
) {
    val typedDetails: ApiErrorDetailsContract get() = ApiErrorDetailsContract.fromMap(details)
}
@Serializable data class LoginRequestContract(val login: String, val password: String)
@Serializable data class OkResponseContract(val ok: Boolean = true)
@Serializable data class CatalogOptionsContract(val tags: JsonElement? = null, val prefixes: JsonElement? = null)
@Serializable data class SavedFilterOptionContract(val id: String = "", val name: String = "")
@Serializable data class SavedFilterRequestContract(
    val id: String = "", val name: String = "", val category: String = "games", val sort: String = "date",
    val search: String = "", val creatorSearch: String = "", val searchMode: String = "title",
    val selectedTags: List<SavedFilterOptionContract> = emptyList(), val excludedTags: List<SavedFilterOptionContract> = emptyList(),
    val selectedPrefixes: List<SavedFilterOptionContract> = emptyList(), val excludedPrefixes: List<SavedFilterOptionContract> = emptyList(),
    val tagType: String = "or", val dateLimit: Int = 0, val expectedUpdatedAt: F95EpochMillis? = null
)
@Serializable data class SavedFilterContract(
    val id: String = "", val name: String = "", val category: String = "games", val sort: String = "date",
    val search: String = "", val creatorSearch: String = "", val searchMode: String = "title",
    val selectedTags: List<SavedFilterOptionContract> = emptyList(), val excludedTags: List<SavedFilterOptionContract> = emptyList(),
    val selectedPrefixes: List<SavedFilterOptionContract> = emptyList(), val excludedPrefixes: List<SavedFilterOptionContract> = emptyList(),
    val tagType: String = "or", val dateLimit: Int = 0, val updatedAt: F95EpochMillis = F95EpochMillis(0L)
)
@Serializable data class QuickLinksBackupContract(val version: Int = 1, val quickLinks: List<SavedFilterRequestContract> = emptyList())
@Serializable data class ImportedQuickLinksContract(val imported: Int = 0)
@Serializable data class MarkRequestContract(
    val trackingState: String, val storedVersion: String? = null, val acknowledge: Boolean = false, val expectedUpdatedAt: F95EpochMillis? = null
)
@Serializable data class MarkResponseContract(
    val threadId: String = "", val trackingState: String = "", val title: String = "", val version: String = "",
    val developer: String = "", val cover: String = "", val prefixes: List<String> = emptyList(), val date: String = "",
    val likes: String = "0", val views: String = "0", val rating: Double = 0.0, val isNew: Boolean = false,
    val storedVersion: String = "", val latestVersion: String? = null, val updateAvailable: Boolean = false, val updatedAt: F95EpochMillis = F95EpochMillis(0L)
)
@Serializable data class DetailMetaContract(val label: String = "", val value: String = "")
@Serializable data class DetailSectionContract(val title: String = "", val html: String = "")
@Serializable data class ThreadDetailContract(
    val threadId: String = "", val title: String = "", val creator: String = "", val date: String = "", val cover: String = "",
    val meta: List<DetailMetaContract> = emptyList(), val overviewHtml: String = "",
    val sections: List<DetailSectionContract> = emptyList(), val bodyHtml: String = ""
)
@Serializable data class RequestMetricsContract(
    val requests: Long = 0,
    val rejected: Long = 0,
    val totalDurationMillis: Long = 0,
    val retries: Long = 0,
    val evictions: Long = 0,
    val circuitOpenings: Long = 0,
    val endpointRequests: Map<String, Long> = emptyMap(),
    val upstream: UpstreamMetricsSnapshotContract = UpstreamMetricsSnapshotContract(),
    val cacheShutdownRejections: Long = 0,
    val cacheWaiterCancellations: Long = 0,
    val cacheActiveLoadInvalidations: Long = 0,
    val cacheStaleLoadCompletions: Long = 0,
    val cacheNamespaces: Map<String, CacheNamespaceMetricsContract> = emptyMap(),
    val countersConsistent: Boolean = true
)
@Serializable data class CacheNamespaceMetricsContract(
    val entries: Long = 0,
    val hits: Long = 0,
    val misses: Long = 0,
    val evictions: Long = 0,
    val expiredEvictions: Long = 0,
    val countersConsistent: Boolean = true
)
@Serializable data class UpstreamMetricsSnapshotContract(
    val requests: Long = 0,
    val responseBytes: Long = 0,
    val retryAttempts: Long = 0,
    val totalDurationMillis: Long = 0,
    val byEndpointAndStatus: Map<String, Long> = emptyMap(),
    val outcomes: UpstreamOutcomeCounts = UpstreamOutcomeCounts(),
    val retryReasons: UpstreamRetryReasonCounts = UpstreamRetryReasonCounts(),
    val retryBudgetExhaustions: Long = 0,
    val retryDeadlineExhaustions: Long = 0,
    val retryDeadlineCancellations: Long = 0,
    val retryCallerCancellations: Long = 0,
    val countersConsistent: Boolean = true
)
@Serializable data class UpstreamOutcomeCounts(
    val success: Long = 0,
    val upstreamFailure: Long = 0,
    val transportFailure: Long = 0,
    val timeout: Long = 0,
    val circuitOpen: Long = 0
)
@Serializable data class UpstreamRetryReasonCounts(
    val httpStatus: Long = 0,
    val ioException: Long = 0
)

