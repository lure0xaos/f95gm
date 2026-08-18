package f95gm.server.config.session

internal fun setCookiePairs(header: String): List<Pair<String, String>> {
    val pairs = mutableListOf<Pair<String, String>>()
    var candidate = 0
    while (candidate >= 0) {
        val nameEnd = cookieNameEnd(header, candidate)
        if (nameEnd != -1 && nameEnd < header.length && header[nameEnd] == '=') {
            val valueStart = nameEnd + 1
            val valueEnd = cookieValueEnd(header, valueStart)
            pairs += header.substring(candidate, nameEnd) to header.substring(valueStart, valueEnd).trim()
            candidate = nextCookiePairStart(header, valueEnd)
        } else {
            candidate = nextCookiePairStart(header, candidate)
        }
    }
    return pairs
}
