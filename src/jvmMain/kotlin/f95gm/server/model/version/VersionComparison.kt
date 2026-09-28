package f95gm.server.model.version

import f95gm.domain.defaults.F95Defaults

internal fun isNewerVersion(latest: String, stored: String): Boolean {
    val current = Version.parseOrNull(latest) ?: return false
    val previous = Version.parseOrNull(stored) ?: return false
    return current > previous
}

@JvmInline
internal value class Version private constructor(val value: String) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        val currentNumbers = versionNumbers(value)
        val previousNumbers = versionNumbers(other.value)
        if (currentNumbers.isNotEmpty() && previousNumbers.isNotEmpty()) {
            val size = maxOf(currentNumbers.size, previousNumbers.size)
            for (index in 0 until size) {
                val comparison = currentNumbers.getOrElse(index) { 0 }
                    .compareTo(previousNumbers.getOrElse(index) { 0 })
                if (comparison != 0) return comparison
            }
            return 0
        }
        return value.compareTo(other.value, ignoreCase = true)
    }

    companion object {
        fun parseOrNull(value: String): Version? = value.takeIf {
            it.isNotBlank() && !it.equals(F95Defaults.UNKNOWN_VERSION, ignoreCase = true)
        }?.let(::Version)
    }
}

private fun versionNumbers(version: String): List<Int> {
    val numbers = mutableListOf<Int>()
    var start = -1
    version.forEachIndexed { index, character ->
        if (character in '0'..'9') {
            if (start == -1) start = index
        } else if (start != -1) {
            version.substring(start, index).toIntOrNull()?.let(numbers::add)
            start = -1
        }
    }
    if (start != -1) version.substring(start).toIntOrNull()?.let(numbers::add)
    return numbers
}
