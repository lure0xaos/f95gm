package f95gm.server.desktop

import java.awt.Color

internal object TrayMenuTheme {
    enum class Mode { LIGHT, DARK }

    fun background(mode: Mode): Color = if (mode == Mode.DARK) Color(0x21, 0x25, 0x29) else Color.WHITE

    fun foreground(mode: Mode): Color = if (mode == Mode.DARK) Color(0xf8, 0xf9, 0xfa) else Color(0x21, 0x25, 0x29)

    fun selectionBackground(mode: Mode): Color =
        if (mode == Mode.DARK) Color(0x34, 0x3a, 0x40) else Color(0xe9, 0xec, 0xef)
}
