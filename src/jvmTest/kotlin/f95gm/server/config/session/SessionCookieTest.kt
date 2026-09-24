package f95gm.server.config.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SessionCookieTest {
    @Test
    fun parsesOnlyAValidSessionCookie() {
        assertEquals(
            "00000000-0000-0000-0000-000000000001",
            parseSessionCookie("other=value; f95gm_session=00000000-0000-0000-0000-000000000001")?.value
        )
        assertNull(parseSessionCookie("f95gm_session=not-a-uuid"))
    }

    @Test
    fun trimsCookiePairsAndRejectsOversizedOrMissingValues() {
        assertEquals(
            "00000000-0000-0000-0000-000000000002",
            parseSessionCookie(" f95gm_session = 00000000-0000-0000-0000-000000000002 ; Path=/")?.value
        )
        assertNull(parseSessionCookie(null))
        assertNull(parseSessionCookie("f95gm_session="))
        assertNull(parseSessionCookie("f95gm_session=${"0".repeat(100)}"))
    }

    @Test
    fun classifiesMissingAndMalformedCookies() {
        assertIs<SessionCookieParseResult.Missing>(parseSessionCookieResult(null))
        assertIs<SessionCookieParseResult.Invalid>(parseSessionCookieResult("f95gm_session=not-a-uuid"))
    }
}
