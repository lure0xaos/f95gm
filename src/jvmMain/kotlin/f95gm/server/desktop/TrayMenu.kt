package f95gm.server.desktop

import f95gm.messages.UiMessages
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.TitledBorder

internal class TrayMenu(
    showConsole: () -> Unit,
    exit: () -> Unit
) {
    private val menuPanel = JPanel()
    private var menuWindow: JWindow? = null
    private var dark = true

    init {
        menuPanel.layout = BoxLayout(menuPanel, BoxLayout.Y_AXIS)
        menuPanel.add(menuItem(UiMessages.app_showConsole(), showConsole))
        menuPanel.add(JSeparator())
        menuPanel.add(menuItem(UiMessages.app_exit(), exit))
        applyTheme()
    }

    fun showAt(point: Point) {
        SwingUtilities.invokeLater {
            hidePopup()
            menuWindow = JWindow().apply {
                background = Color(0, 0, 0, 0)
                focusableWindowState = true
                isAutoRequestFocus = true
                addWindowFocusListener(object : WindowAdapter() {
                    override fun windowLostFocus(event: WindowEvent) {
                        hidePopup()
                    }
                })
                contentPane = menuPanel
                pack()
                location = popupLocation(point, size)
                isVisible = true
                requestFocusInWindow()
            }
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
            hidePopup()
        }
    }

    private fun hidePopup() {
        menuWindow?.dispose()
        menuWindow = null
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
            hidePopup()
            action()
        }
    }

    private fun popupLocation(point: Point, size: Dimension): Point {
        val configuration = screenConfiguration(point)
        val bounds = configuration.bounds
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        val left = bounds.x + insets.left
        val top = bounds.y + insets.top
        val right = bounds.x + bounds.width - insets.right
        val bottom = bounds.y + bounds.height - insets.bottom
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

        menuPanel.background = background
        menuPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(selectionBackground()),
            UiMessages.app_brandName(),
            TitledBorder.CENTER,
            TitledBorder.TOP,
            Font(Font.SANS_SERIF, Font.BOLD, 12),
            foreground
        )
        val items = menuPanel.components.filterIsInstance<JButton>()
        val width = items.maxOfOrNull { it.preferredSize.width } ?: return
        items.forEach { item ->
            item.background = background
            item.foreground = foreground
            item.border = BorderFactory.createEmptyBorder(8, 25, 8, 25)
            item.alignmentX = Component.LEFT_ALIGNMENT
            item.minimumSize = Dimension(width, item.preferredSize.height)
            item.preferredSize = Dimension(width, item.preferredSize.height)
            item.maximumSize = Dimension(width, item.preferredSize.height)
        }
        menuPanel.components.filterIsInstance<JSeparator>().forEach { separator ->
            separator.background = background
            separator.foreground = selectionBackground()
            separator.alignmentX = Component.LEFT_ALIGNMENT
            separator.minimumSize = Dimension(width, separator.preferredSize.height)
            separator.preferredSize = Dimension(width, separator.preferredSize.height)
            separator.maximumSize = Dimension(width, separator.preferredSize.height)
        }
        menuPanel.revalidate()
        menuPanel.repaint()
    }

    private fun menuBackground() = if (dark) Color(0x21, 0x25, 0x29) else Color.WHITE

    private fun menuForeground() = if (dark) Color(0xf8, 0xf9, 0xfa) else Color(0x21, 0x25, 0x29)

    private fun selectionBackground() = if (dark) Color(0x34, 0x3a, 0x40) else Color(0xe9, 0xec, 0xef)
}
