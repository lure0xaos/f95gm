package f95gm.client.model.images

internal data class ImageRetryState(
    val failedSources: Set<String> = emptySet(),
    val attempts: Map<String, Int> = emptyMap()
)
