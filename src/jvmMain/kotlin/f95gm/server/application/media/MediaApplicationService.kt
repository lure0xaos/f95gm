package f95gm.server.application.media

import f95gm.server.config.upstream.mediaHosts
import f95gm.server.config.upstream.UpstreamBaseUrl
import f95gm.server.http.request.SafeHttpsUri
import f95gm.server.http.request.SafeHttpsUriResult
import f95gm.server.upstream.request.UpstreamUrlPolicy
import f95gm.server.model.media.MediaUrl
import f95gm.server.upstream.request.UpstreamPath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal sealed interface MediaFetchResult {
    enum class Rejection { MISSING_OR_INVALID_URL, PRIVATE_HOST, UNSAFE_SCHEME, NO_PUBLIC_ADDRESS, IO_FAILURE, INVALID_RESPONSE }
    data class NotAllowed(val reason: Rejection) : MediaFetchResult
    data class Fetched(val response: MediaFetchResponse) : MediaFetchResult
}

/** Owns media URL validation, DNS revalidation, and the pinned outbound fetch. */
internal class MediaApplicationService(
    private val publicHostResolver: PublicHostResolverPort,
    private val mediaFetcher: MediaFetcher,
    private val upstreamOrigin: UpstreamBaseUrl,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetch(rawUrl: MediaUrl?, cookie: String): MediaFetchResult = withContext(ioDispatcher) {
        val uri = rawUrl?.let {
            when (val result = SafeHttpsUri.parseResult(it.value, mediaHosts)) {
                is SafeHttpsUriResult.Valid -> result.uri
                is SafeHttpsUriResult.Invalid -> null
            }
        } ?: return@withContext MediaFetchResult.NotAllowed(MediaFetchResult.Rejection.MISSING_OR_INVALID_URL)
        if (!publicHostResolver.isFreshPublic(HostName(uri.host))) {
            return@withContext MediaFetchResult.NotAllowed(MediaFetchResult.Rejection.PRIVATE_HOST)
        }
        when (val fetched = mediaFetcher.fetchResult(
                uri,
                MediaCookieHeader(cookie),
                MediaReferrer(UpstreamUrlPolicy.baseUrl(UpstreamPath("/"), upstreamOrigin))
            )) {
            is MediaFetchAttempt.Fetched -> MediaFetchResult.Fetched(fetched.response)
            is MediaFetchAttempt.Rejected -> MediaFetchResult.NotAllowed(
                when (fetched.reason) {
                    MediaFetchRejection.UNSAFE_SCHEME -> MediaFetchResult.Rejection.UNSAFE_SCHEME
                    MediaFetchRejection.NO_PUBLIC_ADDRESS -> MediaFetchResult.Rejection.NO_PUBLIC_ADDRESS
                    MediaFetchRejection.IO_FAILURE -> MediaFetchResult.Rejection.IO_FAILURE
                    MediaFetchRejection.INVALID_RESPONSE -> MediaFetchResult.Rejection.INVALID_RESPONSE
                }
            )
        }
    }
}
