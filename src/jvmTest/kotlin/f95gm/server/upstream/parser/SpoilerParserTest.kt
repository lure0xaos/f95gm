package f95gm.server.upstream.parser

import f95gm.domain.values.F95Html
import f95gm.domain.values.F95OptionName
import f95gm.server.model.detail.DetailSection
import f95gm.server.upstream.parser.spoiler.SpoilerContentMode
import f95gm.server.upstream.parser.spoiler.splitSpoilerSections
import f95gm.server.upstream.parser.spoiler.spoilerHeading
import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpoilerParserTest {
    @Test
    fun trimsSeparatorsAndTrailingBreaksWithoutRemovingContent() {
        val body = Jsoup.parseBodyFragment(":<br>  value<br> ").body()

        trimSectionBody(body)

        assertEquals("value", body.text())
    }

    @Test
    fun recognizesSupportedSpoilerHeadingsOnlyOutsideSpoilers() {
        val document = Jsoup.parseBodyFragment(
            "<b>DOWNLOAD:</b><div class='bbCodeSpoiler'><b>DOWNLOAD:</b></div><b>Unknown:</b>"
        )

        assertEquals("DOWNLOAD", spoilerHeading(document.selectFirst("b")!!))
        assertNull(spoilerHeading(document.select("div.bbCodeSpoiler b").first()!!))
        assertNull(spoilerHeading(document.select("b").last()!!))
    }

    @Test
    fun splitsSpoilersAndExtractsDownloadImages() {
        val section = DetailSection(
            F95OptionName("DESCRIPTION"),
            F95Html(
                """
                    <b>IMPORTANT:</b><div class="bbCodeSpoiler"><div class="bbCodeSpoilerText">Note</div></div>
                    <p>Intro</p>
                    <b>DOWNLOAD:</b><p>Mirror</p><img src="https://example.com/game.jpg">
                """.trimIndent()
            )
        )

        val sections = splitSpoilerSections(section, SpoilerContentMode.KEEP_NON_SPOILER_CONTENT)

        assertEquals(listOf("DESCRIPTION", "IMPORTANT", "DOWNLOAD", "Images"), sections.map { it.title.value })
        assertTrue(sections[0].html.value.contains("Intro"))
        assertTrue(sections[1].html.value.contains("Note"))
        assertTrue(sections[2].html.value.contains("Mirror"))
        assertTrue(sections[3].html.value.contains("game.jpg"))
    }
}
