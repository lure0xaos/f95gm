package f95gm.server.model.auth

import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpstreamSessionTest {
    @Test
    fun rejectsCookieNamesThatCannotBeSafelyWrittenToAHeader() {
        val session = session()

        assertFalse(session.updateCookie("bad name", "value"))
        assertFalse(session.updateCookie("bad\rname", "value"))
        assertTrue(session.typedCookieEntries().isEmpty())
    }

    @Test
    fun rejectsCookieValuesThatContainHeaderDelimitersOrControlCharacters() {
        val session = session()

        assertFalse(session.updateCookie("sid", "value; injected=true"))
        assertFalse(session.updateCookie("sid", "value\r\nX-Injected: true"))
        assertTrue(session.typedCookieEntries().isEmpty())
    }

    private fun session() = UpstreamSession(F95SessionId("browser"), F95AccountKey("account"))
}
