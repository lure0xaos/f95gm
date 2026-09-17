package f95gm.server.routes.marks.live

import f95gm.domain.values.F95ThreadId
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LoadLiveMarksTest {
    @Test
    fun largeBatchBoundsWorkersAndPreservesInputOrder() = runTest {
        val marks = marks(500)
        var active = 0
        var maxActive = 0
        var maxChildren = 0
        val result = loadLiveMarkBatch(marks) { id ->
            val job = checkNotNull(currentCoroutineContext()[Job])
            maxChildren = maxOf(maxChildren, checkNotNull(job.parent).children.count())
            active++
            maxActive = maxOf(maxActive, active)
            try {
                delay(if (id.value.toInt() % 2 == 0) 2L else 1L)
                LiveMarkInfo.fallback(id)
            } finally {
                active--
            }
        }
        assertEquals(marks.map { it.threadId }, result.keys.toList())
        assertTrue(result.all { (id, live) -> id == live.threadId })
        assertEquals(4, maxActive)
        assertTrue(maxChildren <= 5, "Expected at most four workers and one producer, saw $maxChildren")
        assertEquals(0, active)
    }

    @Test
    fun emptyBatchDoesNotInvokeLoaderAndDuplicatesAreRejectedBeforeLoading() = runTest {
        assertTrue(loadLiveMarkBatch(emptyList()) { error("Unexpected load") }.isEmpty())
        var calls = 0
        assertFailsWith<IllegalArgumentException> {
            loadLiveMarkBatch(listOf(marks(1).single(), marks(1).single())) { id ->
                calls++
                LiveMarkInfo.fallback(id)
            }
        }
        assertEquals(0, calls)
    }

    @Test
    fun cancellingBatchStopsWorkersAndBlockedProducer() = runTest {
        var started = 0
        var stopped = 0
        val batch = async {
            loadLiveMarkBatch(marks(500)) {
                started++
                try {
                    awaitCancellation()
                } finally {
                    stopped++
                }
            }
        }
        runCurrent()
        assertEquals(4, started)
        batch.cancelAndJoin()
        assertEquals(started, stopped)
        assertTrue(batch.children.none())
    }

    @Test
    fun loaderFailureUsesFallbackAndDoesNotInterruptOtherMarks() = runTest {
        val completed = mutableSetOf<F95ThreadId>()
        val results = loadLiveMarkBatch(marks(6)) { id ->
            if (id == F95ThreadId("2")) error("upstream failed")
            completed += id
            LiveMarkInfo.fallback(id)
        }
        assertEquals((1..6).map { F95ThreadId(it.toString()) }, results.keys.toList())
        assertEquals((1..6).map { F95ThreadId(it.toString()) }.filterNot { it.value == "2" }.toSet(), completed)
        assertEquals(F95ThreadId("2"), results.getValue(F95ThreadId("2")).threadId)
    }

    @Test
    fun loaderCancellationPropagates() = runTest {
        assertFailsWith<CancellationException> {
            loadLiveMarkBatch(marks(1)) { throw CancellationException("cancel live mark load") }
        }
    }

    private fun marks(count: Int): List<GameMark> = (1..count).map { GameMark(threadId = F95ThreadId(it.toString())) }
}
