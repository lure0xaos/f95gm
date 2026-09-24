package f95gm.client.state.runtime

import kotlinx.browser.window

internal interface BrowserRuntimePort {
    val pathname: String
    val search: String
    fun prefersDarkTheme(): Boolean
}

internal object BrowserRuntime {
    internal var port: BrowserRuntimePort = WindowBrowserRuntime

    val pathname: String get() = port.pathname
    val search: String get() = port.search
    fun prefersDarkTheme(): Boolean = port.prefersDarkTheme()
}

private object WindowBrowserRuntime : BrowserRuntimePort {
    override val pathname: String get() = window.location.pathname
    override val search: String get() = window.location.search
    override fun prefersDarkTheme(): Boolean = window.matchMedia("(prefers-color-scheme: dark)").matches
}
