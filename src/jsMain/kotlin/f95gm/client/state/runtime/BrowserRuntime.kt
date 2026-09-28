package f95gm.client.state.runtime

import kotlinx.browser.window

internal interface BrowserRuntimePort {
    val pathname: String
    val search: String
    fun prefersDarkTheme(): Boolean
}

internal object WindowBrowserRuntime : BrowserRuntimePort {
    override val pathname: String get() = window.location.pathname
    override val search: String get() = window.location.search
    override fun prefersDarkTheme(): Boolean = window.matchMedia("(prefers-color-scheme: dark)").matches
}
