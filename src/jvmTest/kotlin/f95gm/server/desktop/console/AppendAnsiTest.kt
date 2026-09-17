package f95gm.server.desktop.console

import java.awt.Color
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class AppendAnsiTest {
    @Test
    fun appliesStandardAndBrightAnsiColors() {
        val colors = mapOf(
            30 to Color(0x55, 0x55, 0x55), 31 to Color(0xFF, 0x55, 0x55),
            32 to Color(0x55, 0xFF, 0x55), 33 to Color(0xFF, 0xFF, 0x55),
            34 to Color(0x55, 0x55, 0xFF), 35 to Color(0xFF, 0x55, 0xFF),
            36 to Color(0x55, 0xFF, 0xFF), 37 to Color(0xFF, 0xFF, 0xFF),
            90 to Color(0xAA, 0xAA, 0xAA), 91 to Color(0xFF, 0x6B, 0x6B),
            92 to Color(0x6B, 0xFF, 0x6B), 93 to Color(0xFF, 0xFF, 0x6B),
            94 to Color(0x6B, 0x6B, 0xFF), 95 to Color(0xFF, 0x6B, 0xFF),
            96 to Color(0x6B, 0xFF, 0xFF), 97 to Color.WHITE
        )
        colors.forEach { (code, expected) ->
            val document = DefaultStyledDocument()
            appendAnsi(document, "\u001B[${code}mX", Color.BLACK)
            assertEquals(expected, StyleConstants.getForeground(document.getCharacterElement(0).attributes), "ANSI $code")
        }
    }

    @Test
    fun handlesBoldResetDefaultColorMultipleCodesAndPlainText() {
        val document = DefaultStyledDocument()
        val default = Color.BLUE
        appendAnsi(document, "plain\u001B[1;31mred\u001B[22;39mnormal\u001B[mend", default)

        assertEquals("plainrednormalend", document.getText(0, document.length))
        assertEquals(default, StyleConstants.getForeground(document.getCharacterElement(0).attributes))
        assertEquals(true, StyleConstants.isBold(document.getCharacterElement(5).attributes))
        assertEquals(Color(0xFF, 0x55, 0x55), StyleConstants.getForeground(document.getCharacterElement(5).attributes))
        assertEquals(false, StyleConstants.isBold(document.getCharacterElement(8).attributes))
        assertEquals(default, StyleConstants.getForeground(document.getCharacterElement(8).attributes))
        assertEquals(default, StyleConstants.getForeground(document.getCharacterElement(14).attributes))
    }

    @Test
    fun preservesMalformedAndNonSgrEscapeSequencesAsText() {
        val document = DefaultStyledDocument()
        appendAnsi(document, "a\u001B[31b\u001B[unfinished", Color.BLACK)
        assertEquals("a\u001B[31b\u001B[unfinished", document.getText(0, document.length))
    }
}
