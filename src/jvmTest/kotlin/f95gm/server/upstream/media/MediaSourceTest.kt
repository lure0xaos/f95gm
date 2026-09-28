package f95gm.server.upstream.media

import f95gm.server.config.upstream.UpstreamBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaSourceTest {
    private val baseUrl = UpstreamBaseUrl.parse("https://f95zone.to")
    @Test
    fun normalizesSupportedAbsoluteAndProtocolRelativeSources() {
        assertEquals(
            "https://attachments.f95zone.to/files/game.jpg",
            normalizedMediaUrl(" https://attachments.f95zone.to/files/game.jpg ", baseUrl)?.value
        )
        assertEquals(
            "https://preview.f95zone.to/files/game.jpg",
            normalizedMediaUrl("//preview.f95zone.to/files/game.jpg", baseUrl)?.value
        )
        assertNull(normalizedMediaUrl("/files/game.jpg", baseUrl))
    }

    @Test
    fun rejectsUnsupportedSchemesHostsAndMalformedSources() {
        assertNull(normalizedMediaUrl("http://attachments.f95zone.to/files/game.jpg", baseUrl))
        assertNull(normalizedMediaUrl("https://example.com/files/game.jpg", baseUrl))
        assertNull(normalizedMediaUrl("not a uri", baseUrl))
    }

    @Test
    fun buildsProxyUrlsAndAddsThumbnailPathOnlyWhenNeeded() {
        val source = "https://attachments.f95zone.to/files/game image.jpg"

        assertEquals(
            "/api/media?url=https%3A%2F%2Fattachments.f95zone.to%2Ffiles%2Fgame+image.jpg",
            proxyMediaUrl(source)
        )
        assertEquals("", proxyMediaUrl(""))
        assertEquals(
            "https://attachments.f95zone.to/thumb/game.jpg",
            thumbnailMediaSource("https://attachments.f95zone.to/game.jpg")
        )
        assertEquals(
            "https://attachments.f95zone.to/thumb/game.jpg",
            thumbnailMediaSource("https://attachments.f95zone.to/thumb/game.jpg")
        )
        assertEquals("game.jpg", thumbnailMediaSource("game.jpg"))
    }
}
