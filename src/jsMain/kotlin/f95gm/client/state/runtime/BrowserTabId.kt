package f95gm.client.state.runtime

import kotlinx.browser.window
import kotlin.uuid.Uuid

internal object BrowserTabId {
    private const val STORAGE_KEY = "f95gm.browserTabId"

    val value: String = window.sessionStorage.getItem(STORAGE_KEY)
        ?.takeIf { it.isNotBlank() }
        ?: Uuid.random().toString().also { window.sessionStorage.setItem(STORAGE_KEY, it) }
}
