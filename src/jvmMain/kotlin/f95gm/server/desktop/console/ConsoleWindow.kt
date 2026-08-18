package f95gm.server.desktop.console

import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities

internal object ConsoleWindow {
    private val installed = AtomicBoolean(false)
    private val output = ConsoleOutput()
    private var frame: JFrame? = null

    fun installOutputCapture() {
        if (!installed.compareAndSet(false, true)) return
        System.setOut(PrintStream(TeeOutputStream(System.out, output), true))
        System.setErr(PrintStream(TeeOutputStream(System.err, output), true))
    }

    fun show() {
        SwingUtilities.invokeLater {
            val console = frame ?: createFrame().also { frame = it }
            console.isVisible = true
            console.toFront()
            console.requestFocus()
        }
    }

    fun dispose() {
        SwingUtilities.invokeLater {
            frame?.dispose()
            frame = null
        }
    }

    private fun createFrame(): JFrame {
        val textArea = JTextPane().apply {
            isEditable = false
            font = Font("Consolas", Font.PLAIN, 12)
            background = Color.BLACK
            foreground = Color.LIGHT_GRAY
            caretColor = Color.WHITE
            selectionColor = Color(0x33, 0x33, 0x33)
            selectedTextColor = Color.WHITE
            margin = java.awt.Insets(8, 8, 8, 8)
        }
        output.attach(textArea)
        return JFrame("F95GM Console").apply {
            defaultCloseOperation = JFrame.HIDE_ON_CLOSE
            add(JScrollPane(textArea).apply {
                background = Color.BLACK
                viewport.background = Color.BLACK
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            })
            val configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration
            val screen = configuration.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
            val usableWidth = screen.width - insets.left - insets.right
            val usableHeight = screen.height - insets.top - insets.bottom
            setSize(usableWidth * 80 / 100, usableHeight * 75 / 100)
            setLocationRelativeTo(null)
        }
    }
}
