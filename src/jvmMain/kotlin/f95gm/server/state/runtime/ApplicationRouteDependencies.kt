package f95gm.server.state.runtime

import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.state.runtime.cache.BoundedTtlCache

internal interface ApplicationRouteDependencies {
    val applicationServices: RouteApplicationServices
    val liveMarkCache: BoundedTtlCache<String, LiveMarkInfo>
}
