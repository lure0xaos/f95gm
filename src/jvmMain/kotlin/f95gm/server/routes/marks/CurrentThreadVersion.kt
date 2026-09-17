package f95gm.server.routes.marks

import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.routes.marks.live.fetchThreadDetail
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.model.ParseResult

internal suspend fun currentThreadVersion(
    context: MarkRouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): String? {
    return when (val result = fetchThreadDetail(context, threadId, session)) {
        is ParseResult.Success -> result.value.meta.firstOrNull { it.label.value.equals("Version", ignoreCase = true) }
            ?.value?.value?.takeIf { it.isNotBlank() }
        is ParseResult.Invalid -> null
    }
}
