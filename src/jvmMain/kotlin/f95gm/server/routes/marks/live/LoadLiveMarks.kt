package f95gm.server.routes.marks.live

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.state.runtime.RouteDependencies
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val MAX_LIVE_MARK_REQUESTS = 4

internal suspend fun loadLiveMarks(
    context: RouteDependencies,
    marks: List<GameMark>,
    session: UpstreamSession
): Map<String, LiveMarkInfo> = coroutineScope {
    val permits = Semaphore(MAX_LIVE_MARK_REQUESTS)
    marks.map { mark ->
        async { permits.withPermit { mark.threadId.value to loadLiveMark(context, mark.threadId, session) } }
    }.awaitAll().toMap()
}
