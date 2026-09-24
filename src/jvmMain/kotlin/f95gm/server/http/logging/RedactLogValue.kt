package f95gm.server.http.logging

private val sensitiveField = Regex(
    "(?i)([\\\"']?(?:password|passwd|token|cookie|authorization|secret|accountKey|accountId|browserId|sessionId)[\\\"']?\\s*[=:]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^&\\s,}]+)"
)

internal fun redactLogValue(value: String): String =
    sensitiveField.replace(value) { match -> "${match.groupValues[1]}<redacted>" }
        .map { character -> if (character in '\u0000'..'\u001F' || character == '\u007F') ' ' else character }
        .joinToString("")
