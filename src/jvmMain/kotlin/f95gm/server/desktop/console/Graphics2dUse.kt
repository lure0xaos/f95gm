package f95gm.server.desktop.console

import java.awt.Graphics2D

internal fun Graphics2D.use(block: (Graphics2D) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
