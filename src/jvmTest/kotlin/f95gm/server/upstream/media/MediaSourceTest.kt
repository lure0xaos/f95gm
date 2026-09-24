package f95gm.server.upstream.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaSourceTest {
    @Test
    fun normalizesSupportedAbsoluteAndProtocolRelativeSources() {
        assertEquals(
            "https://attachments.f95zone.to/files/game.jpg",
            normalizedMediaSource(" https://attachments.f95zone.to/files/game.jpg ")
        )
        assertEquals(
            "https://preview.f95zone.to/files/game.jpg",
            normalizedMediaSource("//preview.f95zone.to/files/game.jpg")
        )
        assertNull(normalizedMediaSource("/files/game.jpg"))
    }

    @Test
    fun rejectsUnsupportedSchemesHostsAndMalformedSources() {
        assertNull(normalizedMediaSource("http://attachments.f95zone.to/files/game.jpg"))
        assertNull(normalizedMediaSource("https://example.com/files/game.jpg"))
        assertNull(normalizedMediaSource("not a uri"))
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
