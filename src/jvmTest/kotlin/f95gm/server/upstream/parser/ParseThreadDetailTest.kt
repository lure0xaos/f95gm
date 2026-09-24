package f95gm.server.upstream.parser

import f95gm.domain.values.F95ThreadId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseThreadDetailTest {
    @Test
    fun malformedHtmlInputsRemainSafeToParse() {
        val malformedInputs = listOf(
            "",
            "<html><script>",
            "<article class=\"message-body\"><div>",
            "<meta property=\"og:title\" content=\"&lt;broken\"",
            "<\u0000not-html>\u0001"
        )

        malformedInputs.forEach { html ->
            val detail = parseThreadDetail(F95ThreadId("fuzz"), html)
            assertEquals("fuzz", detail.threadId.value)
        }
    }

    @Test
    fun extractsCoreMetadataAndSanitizesBodyMarkup() {
        val detail = parseThreadDetail(
            F95ThreadId("123"),
            """
                <html><head><meta property="og:title" content="Example Game"></head>
                <body><a class="username">Developer</a><time class="u-dt">Today</time>
                <article class="message-body"><div class="bbWrapper">
                    <p>Overview</p><script>alert('x')</script>
                </div></article></body></html>
            """.trimIndent()
        )

        assertEquals("123", detail.threadId.value)
        assertEquals("Example Game", detail.title.value)
        assertEquals("Developer", detail.creator.value)
        assertTrue("alert" !in detail.bodyHtml.value)
    }
}
