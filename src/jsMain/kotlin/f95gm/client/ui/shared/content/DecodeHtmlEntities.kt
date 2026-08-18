package f95gm.client.ui.shared.content

internal fun decodeHtmlEntities(value: String): String = value
    .replace("&#039;", "'")
    .replace("&#39;", "'")
    .replace("&apos;", "'")
    .replace("&quot;", "\"")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
