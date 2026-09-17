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
    private val popup = TrayPopupLifecycle()
    private var themeMode = TrayMenuTheme.Mode.DARK

    init {
        menuPanel.layout = BoxLayout(menuPanel, BoxLayout.Y_AXIS)
        menuPanel.add(menuItem(UiMessages.app_showConsole(), showConsole))
        menuPanel.add(JSeparator())
        menuPanel.add(menuItem(UiMessages.app_exit(), exit))
        applyTheme()
    }

    fun showAt(point: Point) {
        SwingUtilities.invokeLater {
            popup.hide()
            val window = JWindow().apply {
                background = Color(0, 0, 0, 0)
                focusableWindowState = true
                isAutoRequestFocus = true
                addWindowFocusListener(object : WindowAdapter() {
                    override fun windowLostFocus(event: WindowEvent) {
                        popup.hide()
                    }
                })
                contentPane = menuPanel
                pack()
                location = TrayPopupPosition.at(point, size)
                isVisible = true
                requestFocusInWindow()
            }
            popup.show(window)
        }
    }

    fun updateTheme(theme: DesktopTheme?) {
        val nextMode = if (theme?.value == "light") TrayMenuTheme.Mode.LIGHT else TrayMenuTheme.Mode.DARK
        if (themeMode == nextMode) return
        themeMode = nextMode
        SwingUtilities.invokeLater(::applyTheme)
    }

    fun dispose() {
        SwingUtilities.invokeLater {
            popup.hide()
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
            event.component.background = TrayMenuTheme.selectionBackground(themeMode)
            }

            override fun mouseExited(event: MouseEvent) {
            event.component.background = TrayMenuTheme.background(themeMode)
            }
        })
        addActionListener {
            popup.hide()
            action()
        }
    }

    private fun applyTheme() {
        val background = TrayMenuTheme.background(themeMode)
        val foreground = TrayMenuTheme.foreground(themeMode)

        menuPanel.background = background
        menuPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(TrayMenuTheme.selectionBackground(themeMode)),
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
            separator.foreground = TrayMenuTheme.selectionBackground(themeMode)
            separator.alignmentX = Component.LEFT_ALIGNMENT
            separator.minimumSize = Dimension(width, separator.preferredSize.height)
            separator.preferredSize = Dimension(width, separator.preferredSize.height)
            separator.maximumSize = Dimension(width, separator.preferredSize.height)
        }
        menuPanel.revalidate()
        menuPanel.repaint()
    }

}
