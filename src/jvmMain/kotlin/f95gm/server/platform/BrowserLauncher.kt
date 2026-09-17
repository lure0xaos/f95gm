package f95gm.server.platform

import java.net.URI

internal fun interface BrowserLauncher {
    fun open(address: URI)
}
