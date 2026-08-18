package f95gm.server.routes.marks.live

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal suspend fun loadLiveMarks(
    marks: List<GameMark>,
    session: UpstreamSession
): Map<String, LiveMarkInfo> = coroutineScope {
    marks.map { mark ->
        async { mark.threadId.value to loadLiveMark(mark.threadId, session) }
    }.awaitAll().toMap()
}
