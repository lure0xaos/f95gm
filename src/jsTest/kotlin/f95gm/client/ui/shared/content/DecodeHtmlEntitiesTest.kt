package f95gm.client.ui.shared.content

import kotlin.test.Test
import kotlin.test.assertEquals

class DecodeHtmlEntitiesTest {
    @Test
    fun decodesSupportedNamedAndNumericApostropheEntities() {
        assertEquals(
            "Tom & Jerry's \"<game>\"",
            decodeHtmlEntities("Tom &amp; Jerry&#039;s &quot;&lt;game&gt;&quot;")
        )
        assertEquals("It's fine", decodeHtmlEntities("It&apos;s fine"))
    }

    @Test
    fun leavesOrdinaryTextAndUnknownEntitiesUntouched() {
        assertEquals("plain text &unknown;", decodeHtmlEntities("plain text &unknown;"))
    }
}
