package f95gm.server.routes.marks.live

import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.state.runtime.RouteDependencies
import f95gm.server.upstream.parser.parseThreadDetail

internal suspend fun fetchThreadDetail(context: RouteDependencies, threadId: F95ThreadId, session: UpstreamSession) =
    fetchThreadDetailResponse(context, threadId, session)?.let { parseThreadDetail(threadId, it) }
