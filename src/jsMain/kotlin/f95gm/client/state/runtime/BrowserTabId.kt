package f95gm.client.state.runtime

import kotlin.uuid.Uuid

internal class BrowserTabId(private val storage: BrowserStorage) {
    private val storageKey = StorageKey("f95gm.browserTabId")

    val value: String = storage.readSessionOrNull(storageKey)
        ?.takeIf { it.isNotBlank() }
        ?: Uuid.random().toString().also { storage.writeSessionBestEffort(storageKey, it) }
}
