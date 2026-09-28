package f95gm.server.http.logging

import f95gm.server.observability.redactLogValue as redactStructuredLogValue

/** Compatibility facade for HTTP callers; the policy lives in observability. */
internal fun redactLogValue(value: String): String = redactStructuredLogValue(value)
