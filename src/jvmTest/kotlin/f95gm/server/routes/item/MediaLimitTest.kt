package f95gm.server.routes.item

import f95gm.server.config.constants.ServerConstants
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaLimitTest {
    @Test
    fun permitsUnknownAndBoundaryLengthsButRejectsOversizedResponses() {
        assertTrue(mediaResponseWithinLimit(null))
        assertTrue(mediaResponseWithinLimit(ServerConstants.MAX_MEDIA_RESPONSE_BYTES))
        assertFalse(mediaResponseWithinLimit(ServerConstants.MAX_MEDIA_RESPONSE_BYTES + 1))
    }
}
