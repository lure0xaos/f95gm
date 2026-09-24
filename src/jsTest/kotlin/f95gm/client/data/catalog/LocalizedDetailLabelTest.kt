package f95gm.client.data.catalog

import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedDetailLabelTest {
    @Test
    fun localizesSupportedLabelsRegardlessOfCaseAndWhitespace() {
        assertEquals(UiMessages.app_threadUpdated(), localizedDetailLabel("  THREAD UPDATED "))
        assertEquals(UiMessages.app_releaseDate(), localizedDetailLabel("release date"))
        assertEquals(UiMessages.app_download(), localizedDetailLabel("DOWNLOAD"))
        assertEquals(UiMessages.app_developerNotes(), localizedDetailLabel("Developer Notes"))
    }

    @Test
    fun preservesUnknownLabels() {
        assertEquals("Custom Field", localizedDetailLabel("Custom Field"))
    }
}
