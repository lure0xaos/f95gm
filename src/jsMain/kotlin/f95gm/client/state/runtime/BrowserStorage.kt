package f95gm.client.state.runtime

import kotlinx.browser.window

internal interface BrowserStoragePort {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

internal object BrowserStorage {
    internal var localPort: BrowserStoragePort = WindowLocalStorage
    internal var sessionPort: BrowserStoragePort = WindowSessionStorage

    fun read(key: String): String? = runCatching { localPort.read(key) }.getOrNull()

    fun write(key: String, value: String) {
        runCatching { localPort.write(key, value) }
    }

    fun readSession(key: String): String? = runCatching { sessionPort.read(key) }.getOrNull()

    fun writeSession(key: String, value: String) {
        runCatching { sessionPort.write(key, value) }
    }
}

private object WindowLocalStorage : BrowserStoragePort {
    override fun read(key: String): String? = window.localStorage.getItem(key)
    override fun write(key: String, value: String) = window.localStorage.setItem(key, value)
}

private object WindowSessionStorage : BrowserStoragePort {
    override fun read(key: String): String? = window.sessionStorage.getItem(key)
    override fun write(key: String, value: String) = window.sessionStorage.setItem(key, value)
}
