package f95gm.client.state.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserStorageTest {
    @Test
    fun fileTransferPortCanBeExercisedWithoutDom() {
        val previous = BrowserFileTransfer.port
        try {
            var selected: String? = null
            var downloaded: Pair<String, String>? = null
            BrowserFileTransfer.port = object : BrowserFileTransferPort {
                override fun selectTextFile(onLoaded: (String) -> Unit) {
                    onLoaded("{}")
                }

                override fun downloadTextFile(fileName: String, content: String) {
                    downloaded = fileName to content
                }
            }

            BrowserFileTransfer.selectTextFile { selected = it }
            BrowserFileTransfer.downloadTextFile("filters.json", "{}")

            assertEquals("{}", selected)
            assertEquals("filters.json" to "{}", downloaded)
        } finally {
            BrowserFileTransfer.port = previous
        }
    }

    @Test
    fun browserRuntimePortProvidesNavigationAndThemeInputs() {
        val previous = BrowserRuntime.port
        try {
            BrowserRuntime.port = object : BrowserRuntimePort {
                override val pathname = "/settings"
                override val search = "?theme=dark"
                override fun prefersDarkTheme() = true
            }

            assertEquals("/settings", BrowserRuntime.pathname)
            assertEquals("?theme=dark", BrowserRuntime.search)
            assertEquals(true, BrowserRuntime.prefersDarkTheme())
        } finally {
            BrowserRuntime.port = previous
        }
    }

    @Test
    fun storageFailuresAreContainedAndPortsAreReplaceable() {
        val previous = BrowserStorage.localPort
        try {
            var stored: String? = null
            BrowserStorage.localPort = object : BrowserStoragePort {
                override fun read(key: String): String? = stored
                override fun write(key: String, value: String) {
                    stored = value
                }
            }

            BrowserStorage.write("key", "value")

            assertEquals("value", BrowserStorage.read("key"))
        } finally {
            BrowserStorage.localPort = previous
        }
    }

    @Test
    fun storageExceptionsBecomeUnavailableValues() {
        val previous = BrowserStorage.localPort
        try {
            BrowserStorage.localPort = object : BrowserStoragePort {
                override fun read(key: String): String? = error("blocked")
                override fun write(key: String, value: String) = error("blocked")
            }

            assertEquals(null, BrowserStorage.read("key"))
            BrowserStorage.write("key", "value")
        } finally {
            BrowserStorage.localPort = previous
        }
    }
}
