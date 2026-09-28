package f95gm.server.upstream.request

import kotlin.test.Test
import kotlin.test.assertFailsWith

class UpstreamHeaderTest {
    @Test
    fun rejectsControlCharactersAndOversizedValues() {
        assertFailsWith<IllegalArgumentException> { UpstreamHeader("X-Test", "bad\nvalue") }
        assertFailsWith<IllegalArgumentException> { UpstreamHeader("X-Test", "x".repeat(8_193)) }
        assertFailsWith<IllegalArgumentException> { UpstreamHeader("Bad:Name", "value") }
    }
}
