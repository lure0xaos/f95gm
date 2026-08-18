package f95gm.server.desktop.console

import java.awt.Color
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

internal fun insertConsoleText(document: StyledDocument, text: String, color: Color, bold: Boolean) {
    val attributes = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, color)
        StyleConstants.setBold(this, bold)
    }
    document.insertString(document.length, text, attributes)
}
