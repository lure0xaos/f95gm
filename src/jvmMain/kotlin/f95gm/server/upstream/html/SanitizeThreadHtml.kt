package f95gm.server.upstream.html

import f95gm.server.upstream.media.normalizedMediaUrl
import f95gm.server.upstream.media.proxyMediaUrl
import f95gm.server.upstream.media.thumbnailMediaSource
import f95gm.server.upstream.request.UpstreamUrlPolicy
import f95gm.server.upstream.request.UpstreamPath
import f95gm.server.config.upstream.UpstreamBaseUrl
import org.jsoup.Jsoup

internal fun sanitizeThreadHtml(html: String, baseUrl: UpstreamBaseUrl): SanitizedHtml {
    val document = Jsoup.parseBodyFragment(html, UpstreamUrlPolicy.baseUrl(UpstreamPath("/"), baseUrl))
    document.select("script, style, iframe, object, embed, form, button, noscript").remove()
    document.select("*").forEach { element ->
        element.attributes().asList().map { it.key }.forEach { attribute ->
            if (attribute.startsWith("on", ignoreCase = true)) element.removeAttr(attribute)
        }
        if (element.hasAttr("data-src") && !element.hasAttr("src")) {
            element.attr("src", element.attr("data-src"))
        }
        element.removeAttr("data-src")
        element.removeAttr("data-url")
        listOf("src", "href").forEach { attribute ->
            if (!element.hasAttr(attribute)) return@forEach
            val rawValue = element.attr(attribute).trim()
            val value = when {
                rawValue.startsWith("//") -> "https:$rawValue"
            rawValue.startsWith("/") -> UpstreamUrlPolicy.baseUrl(UpstreamPath(rawValue), baseUrl)
                else -> rawValue
            }
            when {
                value.startsWith("javascript:", ignoreCase = true) -> element.removeAttr(attribute)
                attribute == "src" && value.startsWith(
                    "data:image/",
                    ignoreCase = true
                ) -> element.removeAttr(attribute)

                rawValue.startsWith("/") || rawValue.startsWith("//") -> element.attr(attribute, value)
            }
        }
        val style = element.attr("style")
        if (style.contains("javascript:", ignoreCase = true) ||
            style.contains("expression(", ignoreCase = true) ||
            style.contains("url(", ignoreCase = true)
        ) {
            element.removeAttr("style")
        }
    }
    val cleaned = threadHtmlCleaner.clean(document)
    cleaned.outputSettings().prettyPrint(false)
    val result = Jsoup.parseBodyFragment(
        cleaned.body().html(),
        UpstreamUrlPolicy.baseUrl(UpstreamPath("/"), baseUrl)
    )
    result.select("a[href]").forEach { link ->
        val source = normalizedMediaUrl(link.attr("href"), baseUrl) ?: return@forEach
        link.attr("href", proxyMediaUrl(source))
        val image = link.selectFirst("img") ?: return@forEach
        val imageSource = normalizedMediaUrl(image.attr("src"), baseUrl)
            ?: thumbnailMediaSource(source)
        image.attr("src", proxyMediaUrl(imageSource))
    }
    result.select("img[src]").forEach { image ->
        normalizedMediaUrl(image.attr("src"), baseUrl)?.let { source ->
            image.attr("src", proxyMediaUrl(source))
        }
        image.attr("loading", "lazy")
        image.attr("decoding", "async")
    }
    return SanitizedHtml(result.body().html().trim())
}
