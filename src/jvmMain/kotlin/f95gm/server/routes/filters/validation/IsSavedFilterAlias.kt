package f95gm.server.routes.filters.validation

internal fun isSavedFilterAlias(value: String): Boolean {
    if (isUuidShaped(value)) return false
    if (value.isEmpty() || value.first() == '-' || value.last() == '-') return false
    var previousWasDash = false
    value.forEach { character ->
        when {
            character in 'a'..'z' || character in '0'..'9' -> previousWasDash = false
            character == '-' && !previousWasDash -> previousWasDash = true
            else -> return false
        }
    }
    return true
}

private fun isUuidShaped(value: String): Boolean =
    value.length == 36 && value.indices.all { index ->
        when (index) {
            8, 13, 18, 23 -> value[index] == '-'
            else -> value[index].isHexDigit()
        }
    }
