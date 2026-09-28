package f95gm.server.routes.item

import f95gm.server.application.media.MediaApplicationService
import f95gm.server.application.media.MediaFetcher
import f95gm.server.upstream.media.PublicHostResolver
import f95gm.server.routes.ports.MediaResourcePolicy
import kotlinx.coroutines.CoroutineDispatcher

internal interface MediaRouteDependencies {
    val mediaResourcePolicy: MediaResourcePolicy
    val publicHostResolver: PublicHostResolver
    val mediaFetcher: MediaFetcher
    val mediaService: MediaApplicationService
    val ioDispatcher: CoroutineDispatcher
}
