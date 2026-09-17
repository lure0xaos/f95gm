package f95gm.domain.http

/** RFC 3986 query-component encoding shared by browser and server URL builders. */
fun percentEncode(value: String, spaceAsPlus: Boolean = false): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val code = byte.toInt() and 0xff
        if (code == ' '.code && spaceAsPlus) {
            append('+')
        } else if (isQuerySafe(code)) {
            append(code.toChar())
        } else {
            append('%').append(code.toString(16).uppercase().padStart(2, '0'))
        }
    }
}

@Suppress("ComplexCondition")
private fun isQuerySafe(code: Int): Boolean =
    code in 'A'.code..'Z'.code || code in 'a'.code..'z'.code || code in '0'.code..'9'.code ||
        code == '-'.code || code == '_'.code || code == '.'.code || code == '~'.code ||
        code == '!'.code || code == '*'.code || code == '\''.code || code == '('.code || code == ')'.code
