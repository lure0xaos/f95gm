package f95gm.client.data.marks

import f95gm.domain.tracking.GameTrackingState
import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarksContractTest {
    @Test
    fun parsesServerMarkPayloadContract() {
        val marks = parseMarks(
            """[{"threadId":"123","trackingState":"watching","title":"Demo","version":"1.2","developer":"Dev","cover":"/cover.jpg","prefixes":["1"],"date":"Today","likes":"4","views":"20","rating":4.5,"isNew":true,"storedVersion":"1.0","latestVersion":"1.2","updateAvailable":true,"updatedAt":42}]"""
        )

        assertEquals(1, marks.size)
        assertEquals("123", marks.single().threadId.value)
        assertEquals("1.2", marks.single().latestVersion.value)
        assertTrue(marks.single().updateAvailable)
    }

    @Test
    fun appliesSafeDefaultsAndFallsBackFromUnknownTrackingState() {
        val mark = parseMarks("[{\"threadId\":\"456\",\"trackingState\":\"unknown\"}]").single()

        assertEquals(GameTrackingState.WATCHING, mark.trackingState)
        assertEquals(UiMessages.app_untitledGame(), mark.title.value)
        assertEquals("", mark.developer.value)
        assertEquals(0.0, mark.rating)
        assertEquals(false, mark.isNew)
        assertEquals(UiMessages.app_versionUnknown(), mark.storedVersion.value)
        assertEquals(UiMessages.app_versionUnknown(), mark.latestVersion.value)
        assertEquals(0L, mark.updatedAt.value)
    }

    @Test
    fun usesCurrentVersionWhenLatestVersionIsAbsent() {
        val mark = parseMarks("[{\"threadId\":\"789\",\"version\":\"2.0\"}]").single()

        assertEquals("2.0", mark.version.value)
        assertEquals("2.0", mark.latestVersion.value)
    }
}
