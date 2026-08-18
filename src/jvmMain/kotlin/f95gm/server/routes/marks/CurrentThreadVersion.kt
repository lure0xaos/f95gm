package f95gm.server.routes.marks

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.routes.marks.live.fetchThreadDetail

internal suspend fun currentThreadVersion(threadId: String, session: UpstreamSession): String? {
    return fetchThreadDetail(threadId, session)?.meta.orEmpty()
        .firstOrNull { it.label.value.equals("Version", ignoreCase = true) }
        ?.value?.value
        ?.takeIf { it.isNotBlank() }
}
