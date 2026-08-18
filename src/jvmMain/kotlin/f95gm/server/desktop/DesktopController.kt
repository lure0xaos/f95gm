package f95gm.server.desktop

import f95gm.messages.UiMessages
import f95gm.server.config.logging.logger
import f95gm.server.desktop.console.ConsoleWindow
import f95gm.server.desktop.console.use
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal class DesktopController(private val requestShutdown: () -> Unit) {
    private var trayIcon: TrayIcon? = null
    private var trayMenu: TrayMenu? = null

    fun start() {
        runCatching {
            if (!SystemTray.isSupported()) return@runCatching

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
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            trayMenu = menu
            updateTooltip(UiMessages.app_trayServerStarted())
        }.onFailure { error ->
            logger.warn(error) { "Could not initialize the system tray" }
        }
    }

    fun updateTheme(theme: String?) {
        trayMenu?.updateTheme(theme)
    }

    private fun updateTooltip(message: String) {
        runCatching {
            trayIcon?.toolTip = "${UiMessages.app_brandName()} — $message"
        }
    }

    fun dispose() {
        updateTooltip(UiMessages.app_trayServerStopped())
        trayMenu?.dispose()
        trayIcon?.let { icon ->
            runCatching { SystemTray.getSystemTray().remove(icon) }
        }
        trayIcon = null
        trayMenu = null
        ConsoleWindow.dispose()
    }

    companion object {
        fun installConsoleCapture() {
            ConsoleWindow.installOutputCapture()
        }
    }

    private fun trayImage(): Image = runCatching {
        DesktopController::class.java.getResourceAsStream("/static/favicon.png")?.use(ImageIO::read)
    }.getOrNull() ?: fallbackTrayImage()

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
