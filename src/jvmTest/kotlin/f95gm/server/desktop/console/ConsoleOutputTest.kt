package f95gm.server.desktop.console

import java.awt.Color
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.StyleConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsoleOutputTest {
    @Test
    fun acceptsSingleBytesAndUtf8SlicesWhileKeepingTextSnapshot() {
        val output = ConsoleOutput()
        output.write('A'.code)
        val bytes = "é-tail".encodeToByteArray()
        output.write(bytes, 0, 2)
        output.write(bytes, 2, 5)
        assertEquals("Aé-tail", output.snapshot())
    }

    @Test
    fun snapshotRetainsOnlyTheMostRecentOneHundredThousandCharacters() {
        val output = ConsoleOutput()
        output.write(("x".repeat(100_100)).encodeToByteArray())
        assertEquals(100_000, output.snapshot().length)
        assertEquals("x", output.snapshot().take(1))
    }

    @Test
    fun attachingTextPaneRendersExistingAndLaterAnsiFormattedOutput() {
        val output = ConsoleOutput()
        val pane = JTextPane()
        pane.foreground = Color.BLUE
        output.write("before".encodeToByteArray())
        output.attach(pane)
        output.write("\u001B[31mred".encodeToByteArray())
        SwingUtilities.invokeAndWait { }

        assertEquals("beforered", pane.text)
        val red = pane.styledDocument.getCharacterElement(6).attributes
        assertEquals(Color(0xFF, 0x55, 0x55), StyleConstants.getForeground(red))
        assertTrue(pane.caretPosition == pane.document.length)
    }
}
