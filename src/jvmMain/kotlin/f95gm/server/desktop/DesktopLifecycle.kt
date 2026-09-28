package f95gm.server.desktop

/** Platform-neutral lifecycle port consumed by server routes and startup orchestration. */
internal interface DesktopLifecycle {
    fun start()
    fun updateTheme(theme: DesktopTheme?)
    fun dispose()
}

internal object NoopDesktopLifecycle : DesktopLifecycle {
    override fun start() = Unit
    override fun updateTheme(theme: DesktopTheme?) = Unit
    override fun dispose() = Unit
}
