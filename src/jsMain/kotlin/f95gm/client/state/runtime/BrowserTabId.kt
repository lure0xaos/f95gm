package f95gm.client.state.runtime

import kotlin.uuid.Uuid

internal object BrowserTabId {
    private const val STORAGE_KEY = "f95gm.browserTabId"

    val value: String = BrowserStorage.readSession(STORAGE_KEY)
        ?.takeIf { it.isNotBlank() }
        ?: Uuid.random().toString().also { BrowserStorage.writeSession(STORAGE_KEY, it) }
}
