package f95gm.server.http.response

import java.net.URLEncoder

internal fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)
