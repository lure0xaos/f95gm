package f95gm.server.persistence.filters.version

import f95gm.domain.defaults.F95Defaults

internal fun isNewerVersion(latest: String, stored: String): Boolean {
    if (latest.isBlank() || stored.isBlank()) return false
    if (latest.equals(F95Defaults.UNKNOWN_VERSION, ignoreCase = true) ||
        stored.equals(F95Defaults.UNKNOWN_VERSION, ignoreCase = true)
    ) return false
    val latestNumbers = versionNumbers(latest)
    val storedNumbers = versionNumbers(stored)
    if (latestNumbers.isNotEmpty() && storedNumbers.isNotEmpty()) {
        val size = maxOf(latestNumbers.size, storedNumbers.size)
        for (index in 0 until size) {
            val current = latestNumbers.getOrElse(index) { 0 }
            val previous = storedNumbers.getOrElse(index) { 0 }
            if (current != previous) return current > previous
        }
        return false
    }
    return !latest.equals(stored, ignoreCase = true)
}
