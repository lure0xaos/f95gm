package f95gm.server.platform

import java.awt.Desktop
import java.net.URI

internal object DesktopBrowserLauncher : BrowserLauncher {
    override fun open(address: URI) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(address)
        } else {
            throw UnsupportedOperationException("The system does not provide a browser integration")
        }
    }
}
