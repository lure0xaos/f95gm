package f95gm.server.routes.item

import f95gm.server.application.media.ContentLength
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaLimitTest {
    @Test
    fun permitsUnknownAndBoundaryLengthsButRejectsOversizedResponses() {
        val limit = f95gm.server.config.RequestLimits.MAX_MEDIA_RESPONSE_BYTES
        assertTrue(mediaResponseWithinLimit(null, f95gm.server.config.Bytes(limit)))
        assertTrue(mediaResponseWithinLimit(ContentLength(limit), f95gm.server.config.Bytes(limit)))
        assertFalse(mediaResponseWithinLimit(ContentLength(limit + 1), f95gm.server.config.Bytes(limit)))
    }
}
