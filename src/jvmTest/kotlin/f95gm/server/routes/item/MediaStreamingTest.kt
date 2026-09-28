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
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MediaStreamingTest {
    private val policy = MediaResourcePolicy(Bytes(4), MediaDownloadBufferBytes(2))

    @Test
    fun validatesStatusDeclaredLengthAndImageContentType() {
        val response = response(byteArrayOf())
        assertEquals(MediaResponseDecision.ACCEPT, policy.evaluate(response))
        assertEquals(MediaResponseDecision.TOO_LARGE, policy.evaluate(response.copy(contentLength = ContentLength(5))))
        assertEquals(MediaResponseDecision.INVALID_CONTENT_TYPE, policy.evaluate(response.copy(contentType = ContentType.Text.Html)))
        assertEquals(MediaResponseDecision.INVALID_CONTENT_TYPE, policy.evaluate(response.copy(contentType = null)))
        assertEquals(MediaResponseDecision.UPSTREAM_STATUS, policy.evaluate(response.copy(statusCode = HttpStatusValue(302))))
        response.close()
    }

    @Test
    fun servesSvgAsAnAttachmentAndRasterImagesInline() {
        assertEquals("attachment", mediaContentDisposition(ContentType.Image.SVG))
        assertEquals("inline", mediaContentDisposition(ContentType.Image.PNG))
    }

    @Test
    fun streamsExactlyTheLimitAndClosesOnce() = runTest {
        var closes = 0
        val bytes = mutableListOf<Byte>()
        val response = response(byteArrayOf(1, 2, 3, 4)) { closes++ }
        streamMediaResponse(response, policy, Dispatchers.Unconfined) { buffer, count ->
            bytes.addAll(buffer.take(count))
        }
        response.close()
        assertEquals(listOf<Byte>(1, 2, 3, 4), bytes)
        assertEquals(1, closes)
    }

    @Test
    fun rejectsUnknownAndUnderreportedLengthOverflowBeforeWritingExcessBytes() = runTest {
        for (length in listOf(null, ContentLength(1))) {
            var closed = false
            var written = 0
            val response = response(byteArrayOf(1, 2, 3, 4, 5)) { closed = true }.copy(contentLength = length)
            assertFailsWith<MediaResponseTooLargeException> {
                streamMediaResponse(response, policy, Dispatchers.Unconfined) { _, count -> written += count }
            }
            assertEquals(4, written)
            assertTrue(closed)
        }
    }

    @Test
    fun cancellationDuringDownstreamWriteStillClosesUpstream() = runTest {
        val writing = CompletableDeferred<Unit>()
        var closed = false
        val response = response(byteArrayOf(1, 2)) { closed = true }
        val job = launch {
            streamMediaResponse(response, policy, Dispatchers.Unconfined) { _, _ ->
                writing.complete(Unit)
                awaitCancellation()
            }
        }
        writing.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertTrue(closed)
    }

    @Test
    fun downstreamFailureClosesUpstreamAndPropagates() = runTest {
        var closed = false
        val response = response(byteArrayOf(1, 2)) { closed = true }
        assertFailsWith<IOException> {
            streamMediaResponse(response, policy, Dispatchers.Unconfined) { _, _ ->
                throw IOException("downstream disconnected")
            }
        }
        assertTrue(closed)
    }

    private fun response(bytes: ByteArray, onClose: () -> Unit = {}) = MediaFetchResponse(
        HttpStatusValue(200), null, ContentType.Image.PNG, BinaryBody(ByteArrayInputStream(bytes), onClose)
    )
}
