package f95gm.server.routes.filters.validation

internal fun isSavedFilterAlias(value: String): Boolean {
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
