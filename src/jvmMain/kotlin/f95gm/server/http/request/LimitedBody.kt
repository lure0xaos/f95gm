package f95gm.server.http.request

import io.ktor.utils.io.*
import java.io.IOException

internal class BodyLimitExceededException(maxBytes: Long) : IOException("Body exceeded $maxBytes bytes")

internal suspend fun ByteReadChannel.readLimitedBytes(maxBytes: Long): ByteArray {
    require(maxBytes in 1..Int.MAX_VALUE.toLong()) { "maxBytes must fit in an Int" }
    val source = readRemaining(maxBytes + 1)
    val bytes = ByteArray((maxBytes + 1).toInt())
    var count = 0
    while (count < bytes.size && !source.exhausted()) {
        val read = source.readAtMostTo(bytes, count, bytes.size - count)
        if (read <= 0) break
        count += read
    }
    if (count.toLong() > maxBytes) throw BodyLimitExceededException(maxBytes)
    return bytes.copyOf(count)
}
