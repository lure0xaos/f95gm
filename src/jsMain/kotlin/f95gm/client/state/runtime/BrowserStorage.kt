package f95gm.client.state.runtime

import kotlinx.browser.window

internal interface BrowserStoragePort {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

internal sealed interface BrowserStorageResult<out T> {
    data class Success<T>(val value: T) : BrowserStorageResult<T>
    data class Unavailable(val reason: BrowserStorageFailure) : BrowserStorageResult<Nothing>
}

internal enum class BrowserStorageFailure {
    DISABLED,
    SECURITY_RESTRICTION,
    QUOTA_EXCEEDED
}

internal class BrowserStorage(private val runtime: BrowserStorageRuntime) {
    /** Reads with the caller's established fallback when browser storage is unavailable. */
    fun readOrNull(key: StorageKey): String? = BrowserStorageBoundary.read(runtime.localStorage, key.value)
    fun readResult(key: StorageKey): BrowserStorageResult<String?> =
        BrowserStorageBoundary.readResult(runtime.localStorage, key.value)

    /** Persists best-effort state such as UI preferences. */
    fun writeBestEffort(key: StorageKey, value: String) =
        BrowserStorageBoundary.write(runtime.localStorage, key.value, value)
    fun writeResult(key: StorageKey, value: String): BrowserStorageResult<Unit> =
        BrowserStorageBoundary.writeResult(runtime.localStorage, key.value, value)

    fun readSessionOrNull(key: StorageKey): String? = BrowserStorageBoundary.read(runtime.sessionStorage, key.value)
    fun readSessionResult(key: StorageKey): BrowserStorageResult<String?> =
        BrowserStorageBoundary.readResult(runtime.sessionStorage, key.value)

    fun writeSessionBestEffort(key: StorageKey, value: String) =
        BrowserStorageBoundary.write(runtime.sessionStorage, key.value, value)
    fun writeSessionResult(key: StorageKey, value: String): BrowserStorageResult<Unit> =
        BrowserStorageBoundary.writeResult(runtime.sessionStorage, key.value, value)
}

/** Browser storage can be disabled or throw quota/security errors; this is the explicit platform boundary. */
private object BrowserStorageBoundary {
    fun read(port: BrowserStoragePort, key: String): String? = when (val result = readResult(port, key)) {
        is BrowserStorageResult.Success -> result.value
        is BrowserStorageResult.Unavailable -> null
    }

    fun readResult(port: BrowserStoragePort, key: String): BrowserStorageResult<String?> =
        executeStorage { port.read(key) }

    fun write(port: BrowserStoragePort, key: String, value: String) {
        when (writeResult(port, key, value)) {
            is BrowserStorageResult.Success -> Unit
            is BrowserStorageResult.Unavailable -> Unit
        }
    }

    fun writeResult(port: BrowserStoragePort, key: String, value: String): BrowserStorageResult<Unit> =
        executeStorage { port.write(key, value) }

    /** Kotlin/JS exposes DOM storage failures as platform Throwable values; classify only known failures. */
    private inline fun <T> executeStorage(operation: () -> T): BrowserStorageResult<T> =
        BrowserStorageFailureAdapter.execute(operation)

}

/** Named platform adapter confines DOM-specific Throwable inspection to one boundary. */
private object BrowserStorageFailureAdapter {
    @Suppress("TooGenericExceptionCaught") // DOM storage exposes browser-specific failures as generic JS exceptions.
    inline fun <T> execute(operation: () -> T): BrowserStorageResult<T> = try {
        BrowserStorageResult.Success(operation())
    } catch (_: IllegalStateException) {
        BrowserStorageResult.Unavailable(BrowserStorageFailure.DISABLED)
    } catch (error: Exception) {
        error.browserStorageFailure()?.let { BrowserStorageResult.Unavailable(it) } ?: throw error
    }
}

private object WindowLocalStorage : BrowserStoragePort {
    override fun read(key: String): String? = window.localStorage.getItem(key)
    override fun write(key: String, value: String) = window.localStorage.setItem(key, value)
}

internal object WindowStorageRuntime : BrowserStorageRuntime {
    override val localStorage: BrowserStoragePort = WindowLocalStorage
    override val sessionStorage: BrowserStoragePort = WindowSessionStorage
}

private object WindowSessionStorage : BrowserStoragePort {
    override fun read(key: String): String? = window.sessionStorage.getItem(key)
    override fun write(key: String, value: String) = window.sessionStorage.setItem(key, value)
}
