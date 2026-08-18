package f95gm.client.actions.catalog.query

import f95gm.client.state.config.ClientConstants

internal fun queryEncode(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val code = byte.toInt() and ClientConstants.BYTE_MASK
        if (
            code in 'A'.code..'Z'.code ||
            code in 'a'.code..'z'.code ||
            code in '0'.code..'9'.code ||
            code == '-'.code ||
            code == '_'.code ||
            code == '.'.code ||
            code == '!'.code ||
            code == '~'.code ||
            code == '*'.code ||
            code == '\''.code ||
            code == '('.code ||
            code == ')'.code
        ) {
            append(code.toChar())
        } else {
            append('%').append(
                code.toString(ClientConstants.HEX_RADIX).uppercase().padStart(ClientConstants.HEX_DIGITS, '0')
            )
        }
    }
}
