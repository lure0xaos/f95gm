package f95gm.client.state.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class BrowserStorageTest {
    @Test
    fun fileTransferPortCanBeExercisedWithoutDom() {
        var selected: String? = null
        var downloaded: Pair<String, String>? = null
        val effects = object : BrowserEffects by WindowBrowserEffects {
            override fun selectTextFile(onLoaded: (String) -> Unit) {
                onLoaded("{}")
            }

            override fun downloadTextFile(fileName: String, content: String) {
                downloaded = fileName to content
            }
        }

        effects.selectTextFile { selected = it }
        effects.downloadTextFile("filters.json", "{}")

        assertEquals("{}", selected)
        assertEquals("filters.json" to "{}", downloaded)
    }

    @Test
    fun browserRuntimePortProvidesNavigationAndThemeInputs() {
        val effects = object : BrowserEffects by WindowBrowserEffects {
            override val pathname = "/settings"
            override val search = "?theme=dark"
            override fun prefersDarkTheme() = true
        }

        assertEquals("/settings", effects.pathname)
        assertEquals("?theme=dark", effects.search)
        assertEquals(true, effects.prefersDarkTheme())
    }

    @Test
    fun storageUsesInjectedPortsWithoutGlobalReplacement() {
        val values = mutableMapOf<String, String>()
        val storage = BrowserStorage(object : BrowserStorageRuntime {
            override val localStorage = object : BrowserStoragePort {
                override fun read(key: String): String? = values[key]
                override fun write(key: String, value: String) { values[key] = value }
            }
            override val sessionStorage = localStorage
        })

        storage.writeBestEffort(StorageKey("key"), "value")

        assertEquals("value", storage.readOrNull(StorageKey("key")))
    }

    @Test
    fun storageExceptionsBecomeUnavailableValues() {
        val storage = BrowserStorage(object : BrowserStorageRuntime {
            override val localStorage = object : BrowserStoragePort {
                override fun read(key: String): String? = throw browserError("SecurityError")
                override fun write(key: String, value: String) = throw browserError("SecurityError")
            }
            override val sessionStorage = localStorage
        })

        assertEquals(null, storage.readOrNull(StorageKey("key")))
        assertEquals(
            BrowserStorageResult.Unavailable(BrowserStorageFailure.SECURITY_RESTRICTION),
            storage.readResult(StorageKey("key"))
        )
        storage.writeBestEffort(StorageKey("key"), "value")
    }

    @Test
    fun unexpectedStorageExceptionsAreNotSilentlyDiscarded() {
        val storage = BrowserStorage(object : BrowserStorageRuntime {
            override val localStorage = object : BrowserStoragePort {
                override fun read(key: String): String? = throw IllegalArgumentException("unexpected")
                override fun write(key: String, value: String) = throw IllegalArgumentException("unexpected")
            }
            override val sessionStorage = localStorage
        })

        assertFailsWith<IllegalArgumentException> { storage.readOrNull(StorageKey("key")) }
        assertFailsWith<IllegalArgumentException> { storage.writeBestEffort(StorageKey("key"), "value") }
    }

    @Test
    fun unrelatedIllegalStateExceptionsPropagate() {
        val failure = IllegalStateException("storage adapter invariant failed")
        val storage = BrowserStorage(object : BrowserStorageRuntime {
            override val localStorage = object : BrowserStoragePort {
                override fun read(key: String): String? = throw failure
                override fun write(key: String, value: String) = throw failure
            }
            override val sessionStorage = localStorage
        })

        assertSame(failure, assertFailsWith<IllegalStateException> { storage.readOrNull(StorageKey("key")) })
        assertSame(failure, assertFailsWith<IllegalStateException> { storage.writeBestEffort(StorageKey("key"), "value") })
    }

    private fun browserError(name: String): Exception = IllegalStateException("storage unavailable").also {
        it.asDynamic().name = name
    }
}
