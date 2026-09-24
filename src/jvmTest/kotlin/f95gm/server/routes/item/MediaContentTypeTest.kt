package f95gm.server.routes.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MediaContentTypeTest {
    @Test
    fun acceptsValidContentTypesAndRejectsMalformedValues() {
        val valid = assertIs<MediaContentTypeResult.Valid>(parseMediaContentType("image/png"))
        assertEquals("image", valid.value.contentType)
        assertIs<MediaContentTypeResult.Invalid>(parseMediaContentType("not a content type"))
    }
}
