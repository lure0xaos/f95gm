package f95gm.server.routes.marks.live

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.upstream.parser.parseThreadDetail

internal suspend fun fetchThreadDetail(threadId: String, session: UpstreamSession) =
    fetchThreadDetailResponse(threadId, session)?.let { parseThreadDetail(threadId, it) }
