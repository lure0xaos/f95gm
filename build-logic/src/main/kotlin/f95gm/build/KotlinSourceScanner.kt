package f95gm.build

internal data class KotlinCodeRegion(val start: Int, val endExclusive: Int)

/**
 * Keeps source-convention checks focused on Kotlin code rather than comments and
 * string literals. Newlines are preserved so diagnostics retain source lines.
 * Annotation checks may preserve literals while still removing all comments.
 */
internal fun kotlinCode(text: String, preserveLiterals: Boolean = false): String {
    val output = StringBuilder(text.length)
    var index = 0
    var blockCommentDepth = 0
    var quote: Char? = null
    var tripleQuote = false
    while (index < text.length) {
        val current = text[index]
        val next = text.getOrNull(index + 1)
        when {
            blockCommentDepth > 0 && current == '/' && next == '*' -> {
                blockCommentDepth++
                output.append("  ")
                index += 2
            }
            blockCommentDepth > 0 && current == '*' && next == '/' -> {
                blockCommentDepth--
                output.append("  ")
                index += 2
            }
            blockCommentDepth > 0 -> {
                output.append(if (current == '\n') '\n' else ' ')
                index++
            }
            quote == null && current == '/' && next == '/' -> {
                while (index < text.length && text[index] != '\n') {
                    output.append(' ')
                    index++
                }
            }
            quote == null && current == '/' && next == '*' -> {
                blockCommentDepth = 1
                output.append("  ")
                index += 2
            }
            quote == null && current == '"' && text.startsWith("\"\"\"", index) -> {
                quote = '"'
                tripleQuote = true
                output.append(if (preserveLiterals) "\"\"\"" else "   ")
                index += 3
            }
            quote == null && current == '"' -> {
                quote = '"'
                output.append(if (preserveLiterals) current else ' ')
                index++
            }
            quote == null && current == '\'' -> {
                quote = '\''
                output.append(if (preserveLiterals) current else ' ')
                index++
            }
            quote != null && tripleQuote && text.startsWith("\"\"\"", index) -> {
                output.append(if (preserveLiterals) "\"\"\"" else "   ")
                index += 3
                quote = null
                tripleQuote = false
            }
            quote != null && !tripleQuote && current == '\\' -> {
                output.append(if (preserveLiterals) text.substring(index, (index + 2).coerceAtMost(text.length)) else "  ")
                index += 2.coerceAtMost(text.length - index)
            }
            quote != null && !tripleQuote && current == quote -> {
                output.append(if (preserveLiterals) current else ' ')
                index++
                quote = null
            }
            quote != null -> {
                output.append(if (preserveLiterals || current == '\n') current else ' ')
                index++
            }
            else -> {
                output.append(current)
                index++
            }
        }
    }
    return output.toString()
}

/** Extracts actual Kotlin imports while ignoring imports mentioned in comments or strings. */
internal fun kotlinImports(text: String): Set<String> = kotlinCode(text)
    .lineSequence()
    .mapNotNull { line ->
        Regex("^\\s*import\\s+([A-Za-z_][A-Za-z0-9_.]*)").find(line)?.groupValues?.get(1)
    }
    .toSet()

/**
 * Finds calls or lambda receivers while preserving offsets into the original source.
 * The code projection removes comments and literals, so policy checks cannot be
 * satisfied accidentally by text that is not executable Kotlin.
 */
internal fun kotlinCodeRegions(text: String, expression: Regex): List<KotlinCodeRegion> {
    val code = kotlinCode(text)
    return expression.findAll(code).mapNotNull { match ->
        var delimiterStart = match.range.last + 1
        while (delimiterStart < code.length && code[delimiterStart].isWhitespace()) delimiterStart++
        val opening = code.getOrNull(delimiterStart) ?: return@mapNotNull null
        if (opening != '(' && opening != '{' && opening != '[') return@mapNotNull null
        val closing = matchingDelimiter(code, delimiterStart) ?: return@mapNotNull null
        KotlinCodeRegion(match.range.first, closing + 1)
    }.toList()
}

private fun matchingDelimiter(code: String, start: Int): Int? {
    val stack = ArrayDeque<Char>()
    for (index in start until code.length) {
        when (val character = code[index]) {
            '(', '{', '[' -> stack.addLast(character)
            ')', '}', ']' -> {
                val opening = stack.removeLastOrNull() ?: return null
                if (!matches(opening, character)) return null
                if (stack.isEmpty()) return index
            }
        }
    }
    return null
}

private fun matches(opening: Char, closing: Char): Boolean =
    (opening == '(' && closing == ')') ||
        (opening == '{' && closing == '}') ||
        (opening == '[' && closing == ']')
