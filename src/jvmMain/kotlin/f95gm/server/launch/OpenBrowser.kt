package f95gm.server.launch

import f95gm.server.observability.logger
import f95gm.server.platform.BrowserLauncher
import f95gm.server.platform.DesktopBrowserLauncher
import java.io.IOException
import java.net.URI

internal fun openBrowser(address: URI, launcher: BrowserLauncher = DesktopBrowserLauncher) {
    try {
        launcher.open(address)
    } catch (error: IOException) {
        logger.warn(error) { "Could not open the browser at $address" }
    } catch (error: SecurityException) {
        logger.warn(error) { "Could not open the browser at $address" }
    } catch (error: UnsupportedOperationException) {
        logger.warn(error) { "The system does not provide a browser integration; open $address manually" }
    }
}
