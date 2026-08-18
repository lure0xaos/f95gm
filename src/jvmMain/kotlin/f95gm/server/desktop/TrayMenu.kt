package f95gm.server.desktop

import f95gm.messages.UiMessages
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

internal class TrayMenu(
    private val showConsole: () -> Unit,
    private val exit: () -> Unit
) {
    private val popup = JPopupMenu()
    private var anchor: JWindow? = null
    private var dark = true

    init {
        popup.add(menuItem(UiMessages.app_showConsole(), showConsole))
        popup.add(menuItem(UiMessages.app_exit(), exit))
        popup.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(event: PopupMenuEvent) = Unit

            override fun popupMenuWillBecomeInvisible(event: PopupMenuEvent) {
                anchor?.dispose()
                anchor = null
            }

            override fun popupMenuCanceled(event: PopupMenuEvent) {
                anchor?.dispose()
                anchor = null
            }
        })
        applyTheme()
    }

    fun showAt(point: Point) {
        SwingUtilities.invokeLater {
            popup.isVisible = false
            anchor?.dispose()
            val location = popupLocation(point)
            anchor = JWindow().apply {
                background = Color(0, 0, 0, 0)
                setSize(1, 1)
                setLocation(location)
                isVisible = true
            }
            anchor?.let { popup.show(it, 0, 0) }
        }
    }

    fun updateTheme(theme: String?) {
        val nextDark = theme != "light"
        if (dark == nextDark) return
        dark = nextDark
        SwingUtilities.invokeLater(::applyTheme)
    }

    fun dispose() {
        SwingUtilities.invokeLater {
            popup.isVisible = false
            anchor?.dispose()
            anchor = null
        }
    }

    private fun menuItem(label: String, action: () -> Unit) = JButton(label).apply {
        isOpaque = true
        isBorderPainted = false
        isFocusPainted = false
        isRolloverEnabled = false
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        val horizontalPadding = 26
        horizontalAlignment = SwingConstants.CENTER
        border = BorderFactory.createEmptyBorder(9, horizontalPadding, 9, horizontalPadding)
        margin = Insets(0, 0, 0, 0)
        preferredSize = Dimension(getFontMetrics(font).stringWidth(label) + horizontalPadding * 2, 42)
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(event: MouseEvent) {
                event.component.background = selectionBackground()
            }

            override fun mouseExited(event: MouseEvent) {
                event.component.background = menuBackground()
            }
        })
        addActionListener {
            popup.isVisible = false
            action()
        }
    }

    private fun popupLocation(point: Point): Point {
        val configuration = screenConfiguration(point)
        val bounds = configuration.bounds
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        val left = bounds.x + insets.left
        val top = bounds.y + insets.top
        val right = bounds.x + bounds.width - insets.right
        val bottom = bounds.y + bounds.height - insets.bottom
        val size = popup.preferredSize
        val x = (point.x - size.width + 12).coerceIn(left, right - size.width)
        val y = if (point.y > (top + bottom) / 2) {
            point.y - size.height - 4
        } else {
            point.y + 4
        }.coerceIn(top, bottom - size.height)
        return Point(x, y)
    }

    private fun screenConfiguration(point: Point): GraphicsConfiguration =
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .firstOrNull { it.defaultConfiguration.bounds.contains(point) }
            ?.defaultConfiguration
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration

    private fun applyTheme() {
        val background = menuBackground()
        val foreground = menuForeground()

        popup.background = background
        popup.border = BorderFactory.createLineBorder(selectionBackground())
        val items = popup.components.filterIsInstance<JButton>()
        val width = items.maxOfOrNull { it.preferredSize.width } ?: return
        items.forEach { item ->
            item.background = background
            item.foreground = foreground
            item.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selectionBackground()),
                BorderFactory.createEmptyBorder(8, 25, 8, 25)
            )
            item.alignmentX = Component.LEFT_ALIGNMENT
            item.minimumSize = Dimension(width, item.preferredSize.height)
            item.preferredSize = Dimension(width, item.preferredSize.height)
            item.maximumSize = Dimension(width, item.preferredSize.height)
        }
        popup.revalidate()
        popup.repaint()
    }

    private fun menuBackground() = if (dark) Color(0x21, 0x25, 0x29) else Color.WHITE

    private fun menuForeground() = if (dark) Color(0xf8, 0xf9, 0xfa) else Color(0x21, 0x25, 0x29)

    private fun selectionBackground() = if (dark) Color(0x34, 0x3a, 0x40) else Color(0xe9, 0xec, 0xef)
}
