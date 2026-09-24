package f95gm.server.upstream.parser

import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtractSectionsTest {
    @Test
    fun returnsNoSectionsWhenDownloadHeadingIsAbsent() {
        val body = Jsoup.parseBodyFragment("<p>Only description</p>").body()

        assertEquals(emptyList(), extractNestedSections(body))
    }

    @Test
    fun separatesDownloadTextFromTrailingImages() {
        val body = Jsoup.parseBodyFragment(
            "<b>DOWNLOAD:</b><p>Mirror</p><p>Instructions</p>" +
                    "<p><img src='https://example.com/game.jpg'></p>"
        ).body()

        val sections = extractNestedSections(body)

        assertEquals(listOf("DOWNLOAD", "Images"), sections.map { it.title.value })
        assertTrue(sections[0].html.value.contains("Mirror"))
        assertTrue(sections[0].html.value.contains("Instructions"))
        assertTrue(sections[1].html.value.contains("game.jpg"))
        assertTrue(sections[0].html.value.contains("<p>"))
    }

    @Test
    fun ignoresEmptyImageContainersAndKeepsFollowingText() {
        val body = Jsoup.parseBodyFragment(
            "<b>DOWNLOAD:</b><p><span>Not an image</span></p><div></div>"
        ).body()

        val sections = extractNestedSections(body)

        assertEquals(listOf("DOWNLOAD"), sections.map { it.title.value })
        assertTrue(sections.single().html.value.contains("Not an image"))
    }
}
