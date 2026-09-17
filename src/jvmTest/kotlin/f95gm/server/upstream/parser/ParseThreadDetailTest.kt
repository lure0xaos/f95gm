package f95gm.server.upstream.parser

import f95gm.domain.values.F95ThreadId
import f95gm.server.config.upstream.UpstreamBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseThreadDetailTest {
    private val baseUrl = UpstreamBaseUrl.parse("https://f95zone.to")
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
            val detail = parseThreadDetail(F95ThreadId("fuzz"), html, baseUrl)
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
            """.trimIndent(),
            baseUrl
        )

        assertEquals("123", detail.threadId.value)
        assertEquals("Example Game", detail.title.value)
        assertEquals("Developer", detail.creator.value)
        assertTrue("alert" !in detail.bodyHtml.value)
    }
}
