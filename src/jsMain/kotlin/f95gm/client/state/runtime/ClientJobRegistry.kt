package f95gm.client.state.runtime

import kotlinx.coroutines.Job

internal enum class ClientJobKey {
    CATALOG_LOAD,
    JVM_CONNECTION,
    MARKED_GAMES_CHECK,
    SAVED_FILTERS_LOAD,
    VERSION_CHECK
}

internal class ClientJobRegistry {
    private val jobs = mutableMapOf<ClientJobKey, Job>()
    private var closed = false

    fun replace(key: ClientJobKey, job: Job): Job {
        check(!closed) { "Client job registry is closed" }
        jobs.put(key, job)?.cancel()
        job.invokeOnCompletion { removeIfCurrent(key, job) }
        return job
    }

    fun removeIfCurrent(key: ClientJobKey, job: Job) {
        if (jobs[key] === job) jobs.remove(key)
    }

    fun current(key: ClientJobKey): Job? = jobs[key]

    fun isActive(key: ClientJobKey): Boolean = current(key)?.isActive == true

    fun cancel(key: ClientJobKey) {
        jobs.remove(key)?.cancel()
    }

    fun cancelAll() {
        jobs.values.toList().forEach(Job::cancel)
        jobs.clear()
    }

    fun close() {
        if (closed) return
        closed = true
        cancelAll()
    }
}
