package f95gm.server.config.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetCookiePairTest {
    @Test
    fun parsesMultipleCookiesAndIgnoresAttributes() {
        assertEquals(
            listOf("xf_session" to "session-value", "csrf" to "token"),
            setCookiePairs("xf_session=session-value; Path=/; HttpOnly, csrf=token; Secure")
        )
    }

    @Test
    fun preservesEqualsInsideCookieValue() {
        assertEquals(
            listOf("cookie" to "a=b=c"),
            setCookiePairs("cookie=a=b=c; Path=/")
        )
    }

    @Test
    fun ignoresMalformedCandidatesAcrossCookieSeparators() {
        val separators = listOf(',', ';', ',')
        val header = separators.indices.joinToString("") { index ->
            "${separators[index]}${if (index == 1) "broken" else "c$index=v$index"}"
        }
        val parsed = setCookiePairs("first=value$header")
        assertTrue(parsed.first() == ("first" to "value"))
        assertTrue(parsed.any { it.first == "c0" && it.second == "v0" })
    }

    @Test
    fun rejectsControlCharactersAndInvalidCookieNames() {
        assertEquals(
            listOf("valid" to "value"),
            setCookiePairs("valid=value; invalid name=ignored; control=bad\u0001value")
        )
    }
}
