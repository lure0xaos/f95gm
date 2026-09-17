package f95gm.server.state.runtime

import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

import f95gm.server.http.static.StaticResourceCache
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.routes.catalog.CatalogApplicationService
import f95gm.server.routes.item.ItemApplicationService
import f95gm.server.application.media.MediaApplicationService
import f95gm.server.application.media.MediaFetcher
import f95gm.server.upstream.media.PublicHostResolver
import f95gm.server.cache.CacheInvalidator
import f95gm.server.cache.CacheStatisticsProvider
import f95gm.server.cache.CacheAccess
import f95gm.server.cache.ManagedCache
import f95gm.domain.values.F95ThreadId
import f95gm.server.kernel.AwaitableLifecycleResource

internal class ServerRouteServices(
    val staticCache: StaticResourceCache,
    val publicHostResolver: PublicHostResolver,
    val mediaFetcher: MediaFetcher,
    val mediaService: MediaApplicationService,
    val catalogService: CatalogApplicationService,
    val itemService: ItemApplicationService,
    private val liveMarkCacheResource: ManagedCache<F95ThreadId, LiveMarkInfo>,
    val liveMarkCacheAccess: CacheAccess<F95ThreadId, LiveMarkInfo>,
    val liveMarkCacheStatistics: CacheStatisticsProvider
) : AwaitableLifecycleResource {
    private val closeGuard = CloseGuard()
    override val isClosed: Boolean get() = closeGuard.isClosed
    val liveMarkCache: CacheInvalidator<F95ThreadId> get() = liveMarkCacheResource

    override fun close() = closeGuard.closeOnce {
        liveMarkCacheResource.close()
        publicHostResolver.close()
        staticCache.close()
    }

    override suspend fun closeAndJoin() {
        close()
        liveMarkCacheResource.closeAndJoin()
    }
}
