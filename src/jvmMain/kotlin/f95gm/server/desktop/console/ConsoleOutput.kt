package f95gm.server.desktop.console

import java.awt.Color
import java.io.OutputStream
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import f95gm.server.kernel.ResourceLock

internal class ConsoleOutput : OutputStream() {
    private val lock = ResourceLock()
    private val buffer = StringBuilder()
    private var textArea: JTextPane? = null

    fun attach(area: JTextPane) {
        textArea = area
        refresh(area)
    }

    fun snapshot(): String = lock.withLock { buffer.toString() }

    override fun write(byte: Int) {
        write(byteArrayOf(byte.toByte()))
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        val text = bytes.copyOfRange(offset, offset + length).toString(Charsets.UTF_8)
        lock.withLock {
            buffer.append(text)
            if (buffer.length > 100_000) buffer.delete(0, buffer.length - 100_000)
        }
        val area = textArea ?: return
        SwingUtilities.invokeLater {
            refresh(area)
        }
    }

    private fun refresh(area: JTextPane) {
        val document = area.styledDocument
        document.remove(0, document.length)
        appendAnsi(document, snapshot(), area.foreground ?: Color.LIGHT_GRAY)
        area.caretPosition = document.length
    }
}
