package f95gm.server.desktop

import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Toolkit

internal object TrayPopupPosition {
    fun at(point: Point, size: Dimension): Point {
        val configuration = screenConfiguration(point)
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        return within(configuration.bounds, insets, point, size)
    }

    internal fun within(bounds: java.awt.Rectangle, insets: java.awt.Insets, point: Point, size: Dimension): Point {
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
}
