package f95gm.server.upstream.media

import f95gm.server.model.media.MediaUrl
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.defaults.F95Defaults
import f95gm.server.http.response.encode

internal fun proxyMediaUrl(url: String): String =
    if (url.isBlank()) F95Defaults.EMPTY else "${F95ApiPaths.MEDIA}?url=${encode(url)}"

internal fun proxyMediaUrl(url: MediaUrl): String = proxyMediaUrl(url.value)
