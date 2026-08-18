package f95gm.server.desktop.console

import java.awt.Color
import javax.swing.text.StyledDocument

internal fun appendAnsi(document: StyledDocument, text: String, defaultColor: Color) {
    var color = defaultColor
    var bold = false
    var start = 0
    var index = 0
    while (index < text.length) {
        if (text[index] != '\u001B' || index + 1 >= text.length || text[index + 1] != '[') {
            index++
            continue
        }

        if (index > start) insertConsoleText(document, text.substring(start, index), color, bold)
        val end = text.indexOf('m', index + 2)
        if (end < 0) {
            insertConsoleText(document, text.substring(index), color, bold)
            return
        }

        val codes = text.substring(index + 2, end)
        if (codes.isEmpty()) {
            color = defaultColor
            bold = false
        } else {
            var codeStart = 0
            var codeEnd = codes.indexOf(';')
            while (codeStart <= codes.length) {
                if (codeEnd < 0) codeEnd = codes.length
                when (codes.substring(codeStart, codeEnd).toIntOrNull()) {
                    0 -> {
                        color = defaultColor
                        bold = false
                    }

                    1 -> bold = true
                    22 -> bold = false
                    30 -> color = Color(0x55, 0x55, 0x55)
                    31 -> color = Color(0xFF, 0x55, 0x55)
                    32 -> color = Color(0x55, 0xFF, 0x55)
                    33 -> color = Color(0xFF, 0xFF, 0x55)
                    34 -> color = Color(0x55, 0x55, 0xFF)
                    35 -> color = Color(0xFF, 0x55, 0xFF)
                    36 -> color = Color(0x55, 0xFF, 0xFF)
                    37 -> color = Color(0xFF, 0xFF, 0xFF)
                    39 -> color = defaultColor
                    90 -> color = Color(0xAA, 0xAA, 0xAA)
                    91 -> color = Color(0xFF, 0x6B, 0x6B)
                    92 -> color = Color(0x6B, 0xFF, 0x6B)
                    93 -> color = Color(0xFF, 0xFF, 0x6B)
                    94 -> color = Color(0x6B, 0x6B, 0xFF)
                    95 -> color = Color(0xFF, 0x6B, 0xFF)
                    96 -> color = Color(0x6B, 0xFF, 0xFF)
                    97 -> color = Color.WHITE
                }
                if (codeEnd == codes.length) break
                codeStart = codeEnd + 1
                codeEnd = codes.indexOf(';', codeStart)
            }
        }
        index = end + 1
        start = index
    }
    if (start < text.length) insertConsoleText(document, text.substring(start), color, bold)
}
