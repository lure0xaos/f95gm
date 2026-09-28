package f95gm.client.data.detail

import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseDetailTest {
    @Test
    fun mapsDetailMetadataAndSectionsWithLocalizedLabels() {
        val detail = parseDetail(
            """
                {
                  "threadId":"123",
                  "title":"Game",
                  "creator":"Dev",
                  "date":"Today",
                  "cover":"cover.jpg",
                  "meta":[{"label":" developer ","value":"Studio"},{"label":"Custom","value":"Value"}],
                  "overviewHtml":"<p>Overview</p>",
                  "sections":[{"title":"Download","html":"<a>Link</a>"}],
                  "bodyHtml":"<p>Body</p>"
                }
            """.trimIndent()
        )

        assertEquals("123", detail.threadId.value)
        assertEquals("Game", detail.title.value)
        assertEquals(UiMessages.app_developer(), detail.meta[0].label.value)
        assertEquals("Studio", detail.meta[0].value.value)
        assertEquals("Custom", detail.meta[1].label.value)
        assertEquals("Download", detail.sections.single().title.value)
        assertEquals("<p>Body</p>", detail.bodyHtml.value)
    }

    @Test
    fun appliesTitleAndCollectionDefaultsForSparsePayloads() {
        val detail = parseDetail("{\"threadId\":\"42\"}")

        assertEquals("42", detail.threadId.value)
        assertEquals(UiMessages.app_untitledItem(), detail.title.value)
        assertEquals(emptyList(), detail.meta)
        assertEquals(emptyList(), detail.sections)
        assertEquals("", detail.bodyHtml.value)
    }

    @Test
    fun ignoresMalformedCollectionsAndUnknownFields() {
        val detail = parseDetail(
            """{"threadId":"42","meta":{},"sections":"bad","unknown":[1,2]}"""
        )

        assertEquals(emptyList(), detail.meta)
        assertEquals(emptyList(), detail.sections)
    }
}
