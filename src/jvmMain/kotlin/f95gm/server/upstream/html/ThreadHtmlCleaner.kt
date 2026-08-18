package f95gm.server.upstream.html

import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist

internal val threadHtmlCleaner = Cleaner(
    Safelist.relaxed()
        .addTags("center", "details", "figcaption", "figure", "summary")
        .addAttributes(":all", "class", "style", "title")
        .addAttributes("a", "rel", "target")
        .addAttributes("img", "height", "width")
)
