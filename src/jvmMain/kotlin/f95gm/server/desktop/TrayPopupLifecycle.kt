package f95gm.server.desktop

import javax.swing.JWindow

internal class TrayPopupLifecycle {
    private var window: JWindow? = null

    fun show(next: JWindow) {
        hide()
        window = next
    }

    fun hide() {
        window?.dispose()
        window = null
    }
}
