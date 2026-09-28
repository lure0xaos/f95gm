package f95gm.client.model.images

internal data class ImageRetryState(
    val failedSources: Set<MediaSource> = emptySet(),
    val attempts: Map<MediaSource, Int> = emptyMap()
)
