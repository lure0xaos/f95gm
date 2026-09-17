package f95gm.domain.api

import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId
import f95gm.domain.filters.F95Category
import kotlin.jvm.JvmInline

@JvmInline
value class ApiBrowserTabId(val value: String) {
    init { require(value.length in 1..128 && value.all { it.isLetterOrDigit() || it in "-_" }) }
}

@JvmInline
value class ApiProbeNumber(val value: Int) {
    init { require(value >= 0) }
}

@JvmInline
value class ApiTheme(val value: String) {
    init { require(value == "dark" || value == "light") }
}

/** Stable wire paths kept separate from endpoint metadata and schema definitions. */
object F95ApiPaths {
    const val BASE = "/api"
    const val LOGIN = "$BASE/login"
    const val LOGOUT = "$BASE/logout"
    const val SESSION = "$BASE/session"
    const val CATALOG = "$BASE/catalog"
    const val CATALOG_OPTIONS = "$CATALOG/options"
    const val MARKS = "$BASE/marks"
    const val MARKS_CHECK = "$MARKS/check"
    const val FILTERS = "$BASE/filters"
    const val FILTERS_EXPORT = "$FILTERS/export"
    const val FILTERS_IMPORT = "$FILTERS/import"
    const val ITEM = "$BASE/item"
    const val MEDIA = "$BASE/media"
    const val HEALTH = "$BASE/health"
    const val LIVE = "$BASE/live"
    const val READY = "$BASE/ready"
    const val METRICS = "$BASE/metrics"
    const val SHUTDOWN = "$BASE/shutdown"
    const val FILTER_ID = "filterId"
    const val THREAD_ID = "threadId"
    const val MARK_BY_THREAD = "$MARKS/{$THREAD_ID}"
    const val FILTER_BY_ID = "$FILTERS/{$FILTER_ID}"
    const val ITEM_BY_THREAD = "$ITEM/{$THREAD_ID}"

    fun markPath(threadId: F95ThreadId): ApiPath = ApiPath("$MARKS/${threadId.value}")
    fun filterPath(filterId: F95FilterId): ApiPath = ApiPath("$FILTERS/${filterId.value}")
    fun itemPath(threadId: F95ThreadId): ApiPath = ApiPath("$ITEM/${threadId.value}")
    fun catalogOptionsPath(category: F95Category): ApiPath = apiQuery(CATALOG_OPTIONS)
        .add(F95ApiQueryParameters.CATEGORY, category.wireValue)
        .build()

    fun healthPath(probe: ApiProbeNumber, tabId: ApiBrowserTabId, theme: ApiTheme): ApiPath = apiQuery(HEALTH)
        .add(F95ApiQueryParameters.PROBE, probe.value.toString())
        .add(F95ApiQueryParameters.TAB_ID, tabId.value)
        .add(F95ApiQueryParameters.THEME, theme.value)
        .build()

    fun shutdownPath(tabId: ApiBrowserTabId): ApiPath = apiQuery(SHUTDOWN)
        .add(F95ApiQueryParameters.TAB_ID, tabId.value)
        .build()
}
