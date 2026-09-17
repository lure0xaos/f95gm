package f95gm.server.application

import f95gm.domain.api.F95MarkRequest
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95GameVersion
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.application.port.MutationAuditEvent
import f95gm.server.application.port.MutationAuditSink
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.model.marks.LiveMarkInfo
import f95gm.server.testing.FakeMarkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkApplicationServiceTest {
    private val account = F95AccountKey("tester")
    private val threadId = F95ThreadId("19")

    @Test
    fun createMarkPreservesStoredVersionUnlessAcknowledgedAndUsesRequestState() {
        val service = MarkApplicationService(FakeMarkRepository())
        val existing = GameMark(
            threadId = threadId,
            trackingState = GameTrackingState.DOWNLOADED,
            storedVersion = StoredVersion("1.0"),
            lastSeenUpdateVersion = StoredVersion("1.1")
        )

        val preserved = service.createMark(threadId, F95MarkRequest(), existing, EpochMillis(4))
        val acknowledged = service.createMark(
            threadId,
            F95MarkRequest(trackingState = GameTrackingState.PAUSED, storedVersion = StoredVersion(" 2.0 "), acknowledge = true),
            existing,
            EpochMillis(5)
        )

        assertEquals("1.0", preserved.storedVersion.value)
        assertEquals("1.1", preserved.lastSeenUpdateVersion.value)
        assertEquals(GameTrackingState.WATCHING, preserved.trackingState)
        assertEquals("2.0", acknowledged.storedVersion.value)
        assertEquals(GameTrackingState.PAUSED, acknowledged.trackingState)
        assertEquals(EpochMillis(5), acknowledged.updatedAt)
    }

    @Test
    fun resetTrackingStateStoresOnlyNewUnacknowledgedVersions() = runTest {
        val repository = FakeMarkRepository()
        val service = MarkApplicationService(repository)
        val unchanged = GameMark(threadId = F95ThreadId("1"), storedVersion = StoredVersion("1.0"))
        val updated = GameMark(
            threadId = threadId,
            trackingState = GameTrackingState.COMPLETED,
            storedVersion = StoredVersion("1.0")
        )
        repository.store(account, unchanged)
        repository.store(account, updated)
        val live = mapOf(threadId to LiveMarkInfo.fallback(threadId).copy(version = F95GameVersion("1.1")))

        val result = service.resetTrackingStateForNewVersion(account, listOf(unchanged, updated), live)

        assertEquals(GameTrackingState.WATCHING, result.last().trackingState)
        assertEquals("1.1", result.last().lastSeenUpdateVersion.value)
        assertEquals(result, repository.load(account))
    }

    @Test
    fun storingMutationRecordsCompletedAuditEvent() = runTest {
        val repository = FakeMarkRepository()
        val events = mutableListOf<MutationAuditEvent>()
        val service = MarkApplicationService(repository, MutationAuditSink(events::add))
        val mark = GameMark(threadId = threadId)

        service.storeAtomically(account, mark, null)

        assertEquals(mark, repository.load(account).single())
        assertEquals(1, events.size)
    }
}
