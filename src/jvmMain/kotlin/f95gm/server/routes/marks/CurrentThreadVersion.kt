package f95gm.server.routes.marks

import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.routes.marks.live.fetchThreadDetail
import f95gm.server.state.runtime.RouteDependencies

internal suspend fun currentThreadVersion(
    context: RouteDependencies,
    threadId: F95ThreadId,
    session: UpstreamSession
): String? {
    return fetchThreadDetail(context, threadId, session)?.meta.orEmpty()
        .firstOrNull { it.label.value.equals("Version", ignoreCase = true) }
        ?.value?.value
        ?.takeIf { it.isNotBlank() }
}
