package f95gm.server.routes.catalog

import f95gm.domain.api.parsePositiveInt

internal fun isNumericId(value: String): Boolean = value.parsePositiveInt() != null
