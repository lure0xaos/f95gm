package f95gm.server.config.session

internal fun cookieValueEnd(header: String, start: Int): Int {
    var index = start
    while (index < header.length && header[index] != ';' && header[index] != ',') index++
    return index
}
