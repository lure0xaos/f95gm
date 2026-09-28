package f95gm.client.state.runtime

import kotlinx.coroutines.Job
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

internal sealed interface ClientJobKey {
    val value: String

    data object CatalogLoad : ClientJobKey { override val value = "catalog-load" }
    data object JvmConnection : ClientJobKey { override val value = "jvm-connection" }
    data object MarkedGamesCheck : ClientJobKey { override val value = "marked-games-check" }
    data object SavedFiltersLoad : ClientJobKey { override val value = "saved-filters-load" }
    data object VersionCheck : ClientJobKey { override val value = "version-check" }
    data object Authentication : ClientJobKey { override val value = "authentication" }
    data object DetailLoad : ClientJobKey { override val value = "detail-load" }
    data object FilterOptionsLoad : ClientJobKey { override val value = "filter-options-load" }
    data object FilterSave : ClientJobKey { override val value = "filter-save" }
    data object FilterImport : ClientJobKey { override val value = "filter-import" }
    data object FilterExport : ClientJobKey { override val value = "filter-export" }
    data class FilterDelete(val id: F95FilterId) : ClientJobKey { override val value = "filter-delete:${id.value}" }
    data class MarkMutation(val threadId: F95ThreadId) : ClientJobKey { override val value = "mark-mutation:${threadId.value}" }

    companion object {
        val CATALOG_LOAD = CatalogLoad
        val JVM_CONNECTION = JvmConnection
        val MARKED_GAMES_CHECK = MarkedGamesCheck
        val SAVED_FILTERS_LOAD = SavedFiltersLoad
        val VERSION_CHECK = VersionCheck
        val AUTHENTICATION = Authentication
        val DETAIL_LOAD = DetailLoad
        val FILTER_OPTIONS_LOAD = FilterOptionsLoad
        val FILTER_SAVE = FilterSave
        val FILTER_IMPORT = FilterImport
        val FILTER_EXPORT = FilterExport
        fun filterDelete(id: F95FilterId) = FilterDelete(id)
        fun markMutation(threadId: F95ThreadId) = MarkMutation(threadId)
    }
}

internal class ClientJobRegistry {
    private data class Registration(val job: Job, val generation: Long)

    private val jobs = mutableMapOf<ClientJobKey, Registration>()
    private var nextGeneration = 0L
    private var closed = false

    fun replace(key: ClientJobKey, job: Job): Job {
        check(!closed) { "Client job registry is closed" }
        val registration = Registration(job, ++nextGeneration)
        jobs.put(key, registration)?.job?.cancel()
        job.invokeOnCompletion { removeIfCurrent(key, job, registration.generation) }
        return job
    }

    fun removeIfCurrent(key: ClientJobKey, job: Job) {
        jobs[key]?.let { registration ->
            removeIfCurrent(key, job, registration.generation)
        }
    }

    private fun removeIfCurrent(key: ClientJobKey, job: Job, generation: Long) {
        if (jobs[key]?.let { it.job === job && it.generation == generation } == true) jobs.remove(key)
    }

    fun current(key: ClientJobKey): Job? = jobs[key]?.job

    fun isActive(key: ClientJobKey): Boolean = jobs[key]?.job?.isActive == true

    fun cancel(key: ClientJobKey) {
        jobs.remove(key)?.job?.cancel()
    }

    fun cancelAll() {
        jobs.values.map(Registration::job).forEach(Job::cancel)
        jobs.clear()
    }

    fun close() {
        if (closed) return
        closed = true
        cancelAll()
    }
}
