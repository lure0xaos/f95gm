package f95gm.client.actions.catalog.query

import f95gm.domain.http.percentEncode

internal fun queryEncode(value: String): String = percentEncode(value)
