package f95gm.client.ui.detail.metadata

internal fun plainGenreText(html: String): String {
    val withoutTags = buildString {
        var index = 0
        while (index < html.length) {
            if (html[index] != '<') {
                append(html[index])
                index++
                continue
            }
            val tagEnd = html.indexOf('>', index + 1)
            if (tagEnd == -1) {
                append(html[index])
                index++
                continue
            }
            append(' ')
            index = tagEnd + 1
        }
    }.replace("&nbsp;", " ")

    return buildString {
        var whitespacePending = false
        withoutTags.forEach { character ->
            if (character.isWhitespace()) {
                whitespacePending = true
            } else {
                if (whitespacePending && isNotEmpty()) append(' ')
                append(character)
                whitespacePending = false
            }
        }
    }.trim()
}
