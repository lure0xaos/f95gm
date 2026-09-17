package f95gm.server.desktop

import java.awt.TrayIcon
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopControllerTest {
    @Test
    fun repeatedStartAndDisposeInstallAndRemoveOnlyOneTrayIcon() {
        val tray = FakeDesktopTrayHost()
        val controller = DesktopController(requestShutdown = {}, trayHost = tray)

        controller.start()
        controller.start()
        assertEquals(1, tray.added.size)

        controller.dispose()
        controller.dispose()
        controller.start()

        assertEquals(1, tray.removed.size)
        assertEquals(tray.added.single(), tray.removed.single())
    }

    private class FakeDesktopTrayHost : DesktopTrayHost {
        val added = mutableListOf<TrayIcon>()
        val removed = mutableListOf<TrayIcon>()

        override fun isSupported() = true
        override fun add(icon: TrayIcon) { added += icon }
        override fun remove(icon: TrayIcon) { removed += icon }
    }
}
