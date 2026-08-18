package f95gm.server.config.session

internal fun cookieNameEnd(header: String, start: Int): Int {
    if (start >= header.length || !isCookieNameCharacter(header[start])) return -1
    var index = start + 1
    while (index < header.length && isCookieNameCharacter(header[index])) index++
    return index
}
