package f95gm.server.observability

private val sensitiveField = Regex(
    "(?i)([\\\"']?(?:password|passwd|token|cookie|authorization|secret|accountKey|accountId|browserId|sessionId)[\\\"']?\\s*[=:]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^&\\s,}]+)"
)

internal enum class RedactionPolicy { STRUCTURED, URL, COOKIE, SQL_ERROR, CONFIGURATION }

/** Typed redaction policies shared by structured logs, diagnostics, and transport errors. */
internal fun redact(value: String, policy: RedactionPolicy = RedactionPolicy.STRUCTURED): String {
    val normalized = when (policy) {
        RedactionPolicy.URL -> value.substringBefore('?')
        else -> value
    }
    return sensitiveField.replace(normalized) { match -> "${match.groupValues[1]}<redacted>" }
        .map { character -> if (character in '\u0000'..'\u001F' || character == '\u007F') ' ' else character }
        .joinToString("")
}

/** One redaction policy for both structured and legacy text logs. */
internal fun redactLogValue(value: String): String = redact(value)
        .map { character -> if (character in '\u0000'..'\u001F' || character == '\u007F') ' ' else character }
        .joinToString("")
