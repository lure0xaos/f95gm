package f95gm.server.desktop.console

import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.PrintStream
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.CloseGuard
import f95gm.server.kernel.LifecycleResource

internal class ConsoleCaptureHandle(private val restore: () -> Unit) : LifecycleResource {
    private val closeGuard = CloseGuard()

    override val isClosed: Boolean get() = closeGuard.isClosed
    override fun close() = closeGuard.closeOnce(restore)
}

internal object ConsoleWindow {
    private var installed = false
    private val output = ConsoleOutput()
    private var originalOut: PrintStream? = null
    private var originalErr: PrintStream? = null
    private var capturedOut: PrintStream? = null
    private var capturedErr: PrintStream? = null
    private var frame: JFrame? = null
    private val lock = ResourceLock()

    fun installOutputCapture(): ConsoleCaptureHandle = lock.withLock {
        if (installed) return ConsoleCaptureHandle {}
        installed = true
        originalOut = System.out
        originalErr = System.err
        capturedOut = PrintStream(TeeOutputStream(System.out, output), true)
        capturedErr = PrintStream(TeeOutputStream(System.err, output), true)
        System.setOut(capturedOut)
        System.setErr(capturedErr)
        ConsoleCaptureHandle(::restoreOutputCapture)
    }

    fun show() {
        SwingUtilities.invokeLater {
            val console = frame ?: createFrame().also { frame = it }
            console.isVisible = true
            console.toFront()
            console.requestFocus()
        }
    }

    fun dispose() = lock.withLock {
        restoreOutputCapture()
        SwingUtilities.invokeLater {
            frame?.dispose()
            frame = null
        }
    }

    private fun restoreOutputCapture() {
        capturedOut?.flush()
        capturedErr?.flush()
        originalOut?.let(System::setOut)
        originalErr?.let(System::setErr)
        capturedOut = null
        capturedErr = null
        originalOut = null
        originalErr = null
        installed = false
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
        return JFrame("F95 Game Manager Console").apply {
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
