package f95gm.server.routes.item

import f95gm.server.application.media.BinaryBody
import f95gm.server.application.media.ContentLength
import f95gm.server.application.media.MediaFetchResponse
import f95gm.server.config.Bytes
import f95gm.server.config.MediaDownloadBufferBytes
import f95gm.server.model.http.HttpStatusValue
import f95gm.server.routes.ports.MediaResourcePolicy
import io.ktor.http.ContentType
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaResponsePolicyTest {
    private val policy = MediaResourcePolicy(Bytes(10), MediaDownloadBufferBytes(4))

    @Test
    fun acceptsImagesAndClassifiesStatusSizeAndContentTypeFailures() {
        assertEquals(MediaResponseDecision.ACCEPT, response(200, 10, ContentType.Image.PNG).let(policy::evaluate))
        assertEquals(MediaResponseDecision.ACCEPT, response(200, null, ContentType.Image.PNG).let(policy::evaluate))
        assertEquals(MediaResponseDecision.UPSTREAM_STATUS, response(404, 2, ContentType.Image.PNG).let(policy::evaluate))
        assertEquals(MediaResponseDecision.TOO_LARGE, response(200, 11, ContentType.Image.PNG).let(policy::evaluate))
        assertEquals(MediaResponseDecision.INVALID_CONTENT_TYPE, response(200, 2, ContentType.Application.Json).let(policy::evaluate))
        assertEquals(MediaResponseDecision.INVALID_CONTENT_TYPE, response(200, 2, null).let(policy::evaluate))
    }

    @Test
    fun failureResponsesMapToStableStatusAndDispositionRules() {
        assertEquals(404, mediaStatusFailure(404).status.value)
        assertEquals(502, mediaTooLargeFailure().status.value)
        assertEquals(502, mediaTypeFailure().status.value)
        assertEquals("attachment", mediaContentDisposition(ContentType.Image.SVG))
        assertEquals("attachment", mediaContentDisposition(ContentType("image", "SVG+XML")))
        assertEquals("inline", mediaContentDisposition(ContentType.Image.PNG))
        assertEquals("inline", mediaContentDisposition(null))
    }

    private fun response(status: Int, length: Long?, type: ContentType?) = MediaFetchResponse(
        HttpStatusValue(status), length?.let(::ContentLength), type, BinaryBody(ByteArrayInputStream(byteArrayOf()))
    )
}
