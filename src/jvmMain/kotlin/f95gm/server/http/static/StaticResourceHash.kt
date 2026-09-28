package f95gm.server.http.static

import java.security.MessageDigest

internal fun etag(bytes: ByteArray): String =
    "\"${MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }}\""
