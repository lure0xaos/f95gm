package f95gm.server.routes.marks.live

import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.ParseResult
import f95gm.server.model.detail.ThreadDetail
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.upstream.parser.parseThreadDetailResult

internal suspend fun fetchThreadDetail(
    context: MarkRouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): ParseResult<ThreadDetail> = when (val response = fetchThreadDetailResponse(context, threadId, session)) {
    is ParseResult.Invalid -> response
    is ParseResult.Success -> parseThreadDetailResult(threadId, response.value, context.upstreamEndpoints.baseUrl)
}
