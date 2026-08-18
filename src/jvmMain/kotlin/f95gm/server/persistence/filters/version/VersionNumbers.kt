package f95gm.server.persistence.filters.version

internal fun versionNumbers(version: String): List<Int> {
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
