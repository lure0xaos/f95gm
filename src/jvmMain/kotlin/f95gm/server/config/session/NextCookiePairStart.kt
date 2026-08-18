package f95gm.server.config.session

internal fun nextCookiePairStart(header: String, start: Int): Int {
    var comma = header.indexOf(',', start)
    while (comma != -1) {
        var candidate = comma + 1
        while (candidate < header.length && header[candidate].isWhitespace()) candidate++
        val nameEnd = cookieNameEnd(header, candidate)
        if (nameEnd != -1 && nameEnd < header.length && header[nameEnd] == '=') return candidate
        comma = header.indexOf(',', comma + 1)
    }
    return -1
}
