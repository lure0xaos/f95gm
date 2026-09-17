package f95gm.server.routes.ports

import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.cache.CacheAccess
import f95gm.domain.values.F95ThreadId

internal interface LiveMarkRouteDependencies {
    val liveMarkCache: CacheAccess<F95ThreadId, LiveMarkInfo>
}
