package f95gm.server.routes.catalog.parsing

import org.jsoup.Jsoup

internal fun extractJavaScriptObject(source: String, @Suppress("SameParameterValue") variable: String): String? {
    val script = Jsoup.parse(source).select("script").joinToString("\n") { it.data() }
    val assignmentEnd = findJavaScriptAssignment(script, variable) ?: return null
    val start = script.indexOf('{', assignmentEnd)
    if (start < 0) return null

    var depth = 0
    var quote: Char? = null
    var escaped = false
    for (index in start until script.length) {
        val character = script[index]
        if (quote != null) {
            when {
                escaped -> escaped = false
                character == '\\' -> escaped = true
                character == quote -> quote = null
            }
            continue
        }
        when (character) {
            '\"', '\'' -> quote = character
            '{' -> depth += 1
            '}' -> {
                depth -= 1
                if (depth == 0) return script.substring(start, index + 1)
            }
        }
    }
    return null
}
