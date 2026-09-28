package f95gm.server.routes.ports

import f95gm.server.config.Bytes
import f95gm.server.config.MediaDownloadBufferBytes

internal data class MediaResourcePolicy(
    val maxResponseBytes: Bytes,
    val downloadBufferBytes: MediaDownloadBufferBytes
)
