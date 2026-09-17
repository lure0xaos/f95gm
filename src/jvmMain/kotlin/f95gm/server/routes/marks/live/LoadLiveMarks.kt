package f95gm.server.routes.marks.live

import f95gm.server.model.auth.UpstreamSession
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.domain.values.F95ThreadId
import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.failureCategory
import f95gm.server.observability.FailureDescriptor
import f95gm.server.observability.FailureReporter
import f95gm.server.observability.FailureScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAX_LIVE_MARK_REQUESTS = 4

internal suspend fun loadLiveMarks(
    context: MarkRouteDependencies,
    marks: List<GameMark>,
    session: UpstreamSession
): Map<F95ThreadId, LiveMarkInfo> = loadLiveMarkBatch(marks) { threadId ->
    loadLiveMark(context, threadId, session)
}

/** Fixed workers and a bounded queue keep batch scheduling independent of mark count. */
internal suspend fun loadLiveMarkBatch(
    marks: List<GameMark>,
    load: suspend (F95ThreadId) -> LiveMarkInfo
): Map<F95ThreadId, LiveMarkInfo> = coroutineScope {
    require(marks.map { it.threadId }.toSet().size == marks.size) {
        "Live mark batch contains duplicate thread IDs"
    }
    val queue = Channel<IndexedValue<GameMark>>(MAX_LIVE_MARK_REQUESTS)
    val results = arrayOfNulls<LiveMarkInfo>(marks.size)
    try {
        launch {
            marks.forEachIndexed { index, mark -> queue.send(IndexedValue(index, mark)) }
            queue.close()
        }
        List(minOf(MAX_LIVE_MARK_REQUESTS, marks.size)) {
            async {
                for ((index, mark) in queue) {
                    results[index] = loadMarkOrFallback(mark.threadId, load)
                }
            }
        }.awaitAll()
        marks.mapIndexed { index, mark -> mark.threadId to checkNotNull(results[index]) }.toMap()
    } finally {
        queue.cancel()
    }
}

@Suppress("TooGenericExceptionCaught") // Isolates ordinary per-mark upstream failures while preserving cancellation.
private suspend fun loadMarkOrFallback(
    threadId: F95ThreadId,
    load: suspend (F95ThreadId) -> LiveMarkInfo
): LiveMarkInfo = try {
    load(threadId)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    FailureReporter.report(
        FailureScope("live-mark-batch"),
        FailureDescriptor(FailureBoundary.UPSTREAM, failure.failureCategory()),
        failure
    )
    LiveMarkInfo.fallback(threadId)
}
