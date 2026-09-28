@file:Suppress("MatchingDeclarationName") // The snapshot and loader form one marks-refresh contract.

package f95gm.server.routes.marks

import f95gm.domain.values.F95ThreadId
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.routes.marks.live.MarkRouteDependencies
import f95gm.server.routes.marks.live.loadLiveMarks

internal data class RefreshedMarks(
    val marks: List<GameMark>,
    val live: Map<F95ThreadId, LiveMarkInfo>
)

internal suspend fun MarkRouteDependencies.loadAndRefreshMarks(session: UpstreamSession): RefreshedMarks {
    val marks = applicationServices.marks.load(session.accountKey)
    val live = loadLiveMarks(this, marks, session)
    val updatedMarks = applicationServices.marks.resetTrackingStateForNewVersion(
        session.accountKey,
        marks,
        live
    )
    return RefreshedMarks(updatedMarks, live)
}
