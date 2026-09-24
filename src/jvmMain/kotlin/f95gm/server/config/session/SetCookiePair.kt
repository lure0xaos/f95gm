package f95gm.server.config.session

internal fun setCookiePairs(header: String): List<Pair<String, String>> {
    val pairs = mutableListOf<Pair<String, String>>()
    var candidate = 0
    while (candidate >= 0) {
        val nameEnd = cookieNameEnd(header, candidate)
        if (nameEnd != -1 && nameEnd < header.length && header[nameEnd] == '=') {
            val valueStart = nameEnd + 1
            val valueEnd = cookieValueEnd(header, valueStart)
            val name = header.substring(candidate, nameEnd)
            val value = header.substring(valueStart, valueEnd).trim()
            if (name.all(::isCookieNameCharacter) && value.none { it == ';' || it == '\r' || it == '\n' || it.code < 0x20 }) {
                pairs += name to value
            }
            candidate = nextCookiePairStart(header, valueEnd)
        } else {
            candidate = nextCookiePairStart(header, candidate)
        }
    }
    return pairs
}
