package f95gm.server.http.response

import f95gm.domain.http.percentEncode

internal fun encode(value: String): String = percentEncode(value, spaceAsPlus = true)
