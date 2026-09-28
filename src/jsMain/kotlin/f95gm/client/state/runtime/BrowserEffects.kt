package f95gm.client.state.runtime

import kotlinx.browser.document
import kotlinx.browser.window

/** Injectable browser side effects used by client actions and bootstrap. */
internal interface BrowserNavigationEffects {
    val pathname: String
    val search: String
    fun sendBeacon(url: String)
}

internal interface BrowserThemeEffects {
    fun prefersDarkTheme(): Boolean
    fun setTitle(title: String)
}

internal interface BrowserFileEffects {
    fun selectTextFile(onLoaded: (String) -> Unit)
    fun downloadTextFile(fileName: String, content: String)
}

internal interface BrowserEffects : BrowserNavigationEffects, BrowserThemeEffects, BrowserFileEffects {
    val storage: BrowserStorage
}

internal object WindowBrowserEffects : BrowserEffects {
    override val storage: BrowserStorage = BrowserStorage(WindowStorageRuntime)
    override val pathname: String get() = WindowBrowserRuntime.pathname
    override val search: String get() = WindowBrowserRuntime.search
    override fun prefersDarkTheme(): Boolean = WindowBrowserRuntime.prefersDarkTheme()
    override fun setTitle(title: String) { document.title = title }
    override fun sendBeacon(url: String) { window.navigator.asDynamic().sendBeacon(url) }
    override fun selectTextFile(onLoaded: (String) -> Unit) = DomBrowserFileTransfer.selectTextFile(onLoaded)
    override fun downloadTextFile(fileName: String, content: String) =
        DomBrowserFileTransfer.downloadTextFile(fileName, content)
}
