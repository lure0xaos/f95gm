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
            (parseSessionCookieResult("other=value; f95gm_session=00000000-0000-0000-0000-000000000001") as SessionCookieParseResult.Valid).id.value
        )
        assertIs<SessionCookieParseResult.Invalid>(parseSessionCookieResult("f95gm_session=not-a-uuid"))
    }

    @Test
    fun trimsCookiePairsAndRejectsOversizedOrMissingValues() {
        assertEquals(
            "00000000-0000-0000-0000-000000000002",
            (parseSessionCookieResult(" f95gm_session = 00000000-0000-0000-0000-000000000002 ; Path=/") as SessionCookieParseResult.Valid).id.value
        )
        assertIs<SessionCookieParseResult.Missing>(parseSessionCookieResult(null))
        assertIs<SessionCookieParseResult.Invalid>(parseSessionCookieResult("f95gm_session="))
        assertIs<SessionCookieParseResult.Invalid>(parseSessionCookieResult("f95gm_session=${"0".repeat(100)}"))
    }

    @Test
    fun classifiesMissingAndMalformedCookies() {
        assertIs<SessionCookieParseResult.Missing>(parseSessionCookieResult(null))
        assertIs<SessionCookieParseResult.Invalid>(parseSessionCookieResult("f95gm_session=not-a-uuid"))
    }
}
