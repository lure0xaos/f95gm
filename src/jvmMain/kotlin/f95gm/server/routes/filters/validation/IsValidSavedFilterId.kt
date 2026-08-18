package f95gm.server.routes.filters.validation

import f95gm.server.config.constants.ServerConstants

internal fun isValidSavedFilterId(value: String): Boolean =
    value.length <= ServerConstants.FILTER_ID_LENGTH &&
            (isSavedFilterAlias(value) || isLegacySavedFilterId(value))
