package f95gm.server.state.runtime

import f95gm.server.routes.item.MediaFetcher
import f95gm.server.routes.item.PublicHostResolver

internal interface MediaRouteDependencies {
    val publicHostResolver: PublicHostResolver
    val mediaFetcher: MediaFetcher
}
