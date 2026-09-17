package f95gm.server.upstream.media

import f95gm.server.model.media.MediaUrl
internal fun thumbnailMediaSource(source: String): String {
    val slash = source.lastIndexOf('/')
    return if (slash < 0 || source.substring(0, slash).endsWith("/thumb")) {
        source
    } else {
        source.substring(0, slash) + "/thumb" + source.substring(slash)
    }
}

internal fun thumbnailMediaSource(source: MediaUrl): MediaUrl = MediaUrl(thumbnailMediaSource(source.value))
