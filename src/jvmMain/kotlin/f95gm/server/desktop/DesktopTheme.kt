package f95gm.server.desktop

@JvmInline
internal value class DesktopTheme(val value: String) {
    init { require(value.isNotBlank()) }
}
