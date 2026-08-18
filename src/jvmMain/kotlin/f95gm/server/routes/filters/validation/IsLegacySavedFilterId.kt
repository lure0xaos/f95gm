package f95gm.server.routes.filters.validation

internal fun isLegacySavedFilterId(value: String): Boolean =
    value.length == 36 && value.indices.all { index ->
        when (index) {
            8, 13, 18, 23 -> value[index] == '-'
            else -> value[index].isHexDigit()
        }
    }
