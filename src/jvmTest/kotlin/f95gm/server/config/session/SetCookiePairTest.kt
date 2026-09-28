package f95gm.server.config.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetCookiePairTest {
    @Test
    fun parsesMultipleCookiesAndIgnoresAttributes() {
        assertEquals(
            listOf(SetCookiePair("xf_session", "session-value"), SetCookiePair("csrf", "token")),
            setCookiePairs("xf_session=session-value; Path=/; HttpOnly, csrf=token; Secure")
        )
    }

    @Test
    fun preservesEqualsInsideCookieValue() {
        assertEquals(
            listOf(SetCookiePair("cookie", "a=b=c")),
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
        assertTrue(parsed.first() == SetCookiePair("first", "value"))
        assertTrue(parsed.any { it.name == "c0" && it.value == "v0" })
    }

    @Test
    fun rejectsControlCharactersAndInvalidCookieNames() {
        assertEquals(
            listOf(SetCookiePair("valid", "value")),
            setCookiePairs("valid=value; invalid name=ignored; control=bad\u0001value")
        )
    }
}
