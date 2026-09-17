package f95gm.server.application.media

import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.model.http.HttpStatusValue
import f95gm.server.model.media.MediaUrl
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.net.URI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MediaApplicationServiceTest {
    @Test
    fun validatesTheUrlAndRevalidatesTheHostBeforeFetching() = runTest {
        var fetches = 0
        val resolver = fakeResolver(public = false)
        val fetcher = MediaFetcher { _, _, _ ->
            fetches += 1
            error("fetch must not run for a private host")
        }
        val service = MediaApplicationService(
            resolver, fetcher, UpstreamBaseUrl.parse("https://f95zone.to"), Dispatchers.Unconfined
        )

        assertEquals(
            MediaFetchResult.Rejection.PRIVATE_HOST,
            (service.fetch(MediaUrl("https://attachments.f95zone.to/file.png"), "cookie") as MediaFetchResult.NotAllowed).reason
        )
        assertEquals(0, fetches)
    }

    @Test
    fun passesOnlyValidatedMediaRequestsToTheFetcher() = runTest {
        var requestedUri: URI? = null
        var requestedCookie: String? = null
        val response = MediaFetchResponse(
            statusCode = HttpStatusValue(200),
            contentLength = null,
            contentType = null,
            body = BinaryBody(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        )
        val service = MediaApplicationService(
            fakeResolver(public = true),
            MediaFetcher { uri, cookie, _ ->
                requestedUri = uri
                requestedCookie = cookie.value
                MediaFetchAttempt.Fetched(response)
            },
            UpstreamBaseUrl.parse("https://f95zone.to"),
            Dispatchers.Unconfined
        )

        val result = assertIs<MediaFetchResult.Fetched>(
            service.fetch(MediaUrl("https://attachments.f95zone.to/file.png"), "session-cookie")
        )

        assertEquals("https://attachments.f95zone.to/file.png", requestedUri.toString())
        assertEquals("session-cookie", requestedCookie)
        assertEquals(response, result.response)
        result.response.close()
    }

    @Test
    fun rejectsMalformedAndDisallowedMediaUrlsBeforeDnsRevalidation() = runTest {
        var resolutions = 0
        val resolver = object : PublicHostResolverPort {
            override fun isFreshPublic(host: HostName): Boolean {
                resolutions += 1
                return true
            }

            override fun freshPublicAddresses(host: HostName): List<InetAddress> = emptyList()
        }
        val service = MediaApplicationService(
            resolver,
            MediaFetcher { _, _, _ -> error("fetch must not run") },
            UpstreamBaseUrl.parse("https://f95zone.to"),
            Dispatchers.Unconfined
        )

        val result = service.fetch(MediaUrl("http://attachments.f95zone.to/file.png"), "")

        assertEquals(MediaFetchResult.Rejection.MISSING_OR_INVALID_URL, (result as MediaFetchResult.NotAllowed).reason)
        assertEquals(0, resolutions)
    }

    @Test
    fun performsDnsAndConnectionSetupOnTheInjectedIoDispatcher() = runTest {
        val dispatcher = TrackingDispatcher()
        var resolverRanOnDispatcher = false
        var fetchRanOnDispatcher = false
        val resolver = object : PublicHostResolverPort {
            override fun isFreshPublic(host: HostName): Boolean {
                resolverRanOnDispatcher = dispatcher.isExecuting
                return true
            }

            override fun freshPublicAddresses(host: HostName): List<InetAddress> = emptyList()
        }
        val service = MediaApplicationService(
            resolver,
            MediaFetcher { _, _, _ ->
                fetchRanOnDispatcher = dispatcher.isExecuting
                MediaFetchAttempt.Rejected(MediaFetchRejection.NO_PUBLIC_ADDRESS)
            },
            UpstreamBaseUrl.parse("https://f95zone.to"),
            dispatcher
        )

        service.fetch(MediaUrl("https://attachments.f95zone.to/file.png"), "")

        assertEquals(true, resolverRanOnDispatcher)
        assertEquals(true, fetchRanOnDispatcher)
    }

    private fun fakeResolver(public: Boolean): PublicHostResolverPort = object : PublicHostResolverPort {
        override fun isFreshPublic(host: HostName): Boolean = public
        override fun freshPublicAddresses(host: HostName): List<InetAddress> = emptyList()
    }

    private class TrackingDispatcher : CoroutineDispatcher() {
        var isExecuting = false
            private set

        override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
            isExecuting = true
            try {
                block.run()
            } finally {
                isExecuting = false
            }
        }
    }
}
