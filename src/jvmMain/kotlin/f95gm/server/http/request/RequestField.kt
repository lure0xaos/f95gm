package f95gm.server.http.request

import java.net.URLDecoder

internal fun requestField(body: String, field: String): String? =
    body.split('&')
        .asSequence()
        .map { it.split('=', limit = 2) }
        .firstOrNull { it.firstOrNull() == field }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, Charsets.UTF_8) }
