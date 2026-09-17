package f95gm.server.desktop

import f95gm.messages.UiMessages
import f95gm.server.observability.logger
import f95gm.server.desktop.console.ConsoleWindow
import f95gm.server.desktop.console.ConsoleCaptureHandle
import f95gm.server.desktop.console.use
import f95gm.server.kernel.ResourceLock
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO

internal interface DesktopTrayHost {
    fun isSupported(): Boolean
    fun add(icon: TrayIcon)
    fun remove(icon: TrayIcon)

    object Awt : DesktopTrayHost {
        override fun isSupported(): Boolean = SystemTray.isSupported()
        override fun add(icon: TrayIcon) = SystemTray.getSystemTray().add(icon)
        override fun remove(icon: TrayIcon) = SystemTray.getSystemTray().remove(icon)
    }
}

internal class DesktopController(
    private val requestShutdown: () -> Unit,
    private val trayHost: DesktopTrayHost = DesktopTrayHost.Awt
) : DesktopLifecycle {
    private var trayIcon: TrayIcon? = null
    private var trayMenu: TrayMenu? = null
    private val lifecycleLock = ResourceLock()
    private var started = false
    private var disposed = false

    override fun start() = lifecycleLock.withLock {
        if (started || disposed) return@withLock
        if (!trayHost.isSupported()) return@withLock
        try {
            val menu = TrayMenu(ConsoleWindow::show, requestShutdown)
            val icon = TrayIcon(trayImage(), UiMessages.app_brandName()).apply {
                isImageAutoSize = true
                addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(event: MouseEvent) {
                        if (event.isPopupTrigger) menu.showAt(event.locationOnScreen)
                    }

                    override fun mouseReleased(event: MouseEvent) {
                        if (event.isPopupTrigger) menu.showAt(event.locationOnScreen)
                    }
                })
            }
            trayHost.add(icon)
            trayIcon = icon
            trayMenu = menu
            started = true
            updateTooltip(UiMessages.app_trayServerStarted())
        } catch (error: AWTException) {
            logger.warn(error) { "Could not initialize the system tray" }
        } catch (error: IllegalArgumentException) {
            logger.warn(error) { "Could not initialize the system tray" }
        }
    }

    override fun updateTheme(theme: DesktopTheme?) {
        lifecycleLock.withLock { trayMenu?.updateTheme(theme) }
    }

    private fun updateTooltip(message: String) {
        try {
            trayIcon?.toolTip = "${UiMessages.app_brandName()} — $message"
        } catch (error: IllegalArgumentException) {
            // AWT can reject updates while the desktop session is shutting down.
            logger.debug(error) { "Could not update the system-tray tooltip" }
        }
    }

    override fun dispose() = lifecycleLock.withLock {
        if (disposed) return@withLock
        disposed = true
        updateTooltip(UiMessages.app_trayServerStopped())
        trayMenu?.dispose()
        trayIcon?.let { icon ->
            try {
                trayHost.remove(icon)
            } catch (error: IllegalArgumentException) {
                logger.debug(error) { "Could not remove the system-tray icon" }
            }
        }
        trayIcon = null
        trayMenu = null
        ConsoleWindow.dispose()
    }

    companion object {
        fun installConsoleCapture(): ConsoleCaptureHandle {
            return ConsoleWindow.installOutputCapture()
        }
    }

    private fun trayImage(): Image = try {
        DesktopController::class.java.getResourceAsStream("/static/favicon.png")?.use(ImageIO::read)
    } catch (error: IOException) {
        logger.debug(error) { "Could not load the system-tray image" }
        null
    } ?: fallbackTrayImage()

    private fun fallbackTrayImage(): Image {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.use {
            it.color = Color(0x20, 0x25, 0x2f)
            it.fillRoundRect(0, 0, 32, 32, 8, 8)
            it.color = Color.WHITE
            it.font = Font(Font.SANS_SERIF, Font.BOLD, 22)
            it.drawString("F", 9, 24)
        }
        return image
    }
}
