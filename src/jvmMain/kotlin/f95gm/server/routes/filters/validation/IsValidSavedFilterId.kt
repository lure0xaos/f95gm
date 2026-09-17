package f95gm.server.routes.filters.validation


internal fun isValidSavedFilterId(value: String, maxLength: Int = 64): Boolean =
    value.length <= maxLength && isSavedFilterAlias(value)
