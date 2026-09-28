package f95gm.server.upstream.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import f95gm.server.upstream.parser.shared.HtmlBaseUrl

class ParseDetailBodyTest {
    @Test
    fun separatesOverviewAndRecognizedSectionsCaseInsensitively() {
        val parsed = parseDetailBody(
            """
                <p>Overview text</p>
                <b>developer:</b><p>Jane</p>
                <b>Unknown:</b><p>Still developer content</p>
                <b>VERSION:</b><p>1.2</p>
            """.trimIndent(),
            HtmlBaseUrl("https://f95zone.to")
        )

        assertTrue(parsed.overview.contains("Overview text"))
        assertEquals(listOf("developer", "VERSION"), parsed.sections.map { it.title.value })
        assertTrue(parsed.sections[0].html.value.contains("Jane"))
        assertTrue(parsed.sections[0].html.value.contains("Unknown"))
        assertTrue(parsed.sections[1].html.value.contains("1.2"))
    }

    @Test
    fun omitsEmptySectionsAndKeepsUnknownOnlyMarkupInOverview() {
        val parsed = parseDetailBody(
            """
                <p>Intro</p><b>Developer:</b><br><b>Language:</b><p>English</p>
            """.trimIndent(),
            HtmlBaseUrl("https://f95zone.to")
        )

        assertTrue(parsed.overview.contains("Intro"))
        assertFalse(parsed.sections.any { it.title.value.equals("Developer", ignoreCase = true) })
        assertTrue(parsed.sections.any { it.title.value.equals("Language", ignoreCase = true) })
    }
}
