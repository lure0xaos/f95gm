package f95gm.server.launch

import f95gm.server.config.logging.logger
import java.awt.Desktop
import java.net.URI

internal fun openBrowser(address: URI) {
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(address)
        } else {
            logger.warn { "The system does not provide a browser integration; open $address manually" }
        }
    }.onFailure { error ->
        logger.warn(error) { "Could not open the browser at $address" }
    }
}
