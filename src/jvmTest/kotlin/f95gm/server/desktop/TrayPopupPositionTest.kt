package f95gm.server.desktop

import java.awt.Dimension
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class TrayPopupPositionTest {
    private val bounds = Rectangle(100, 100, 800, 600)
    private val insets = Insets(10, 5, 20, 15)

    @Test
    fun popupStaysInsideUsableAreaAndAvoidsTheTrayIcon() {
        assertEquals(Point(105, 110), TrayPopupPosition.within(bounds, insets, Point(10, 20), Dimension(120, 80)))
        assertEquals(Point(765, 600), TrayPopupPosition.within(bounds, insets, Point(890, 690), Dimension(120, 80)))
        assertEquals(Point(392, 304), TrayPopupPosition.within(bounds, insets, Point(500, 300), Dimension(120, 80)))
        assertEquals(Point(392, 416), TrayPopupPosition.within(bounds, insets, Point(500, 500), Dimension(120, 80)))
    }
}
