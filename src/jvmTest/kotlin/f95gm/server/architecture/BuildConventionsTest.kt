package f95gm.server.architecture

import f95gm.server.http.response.ServerMessageKey
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.readLines
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildConventionsTest {
    private val sourceRoot = Path.of(System.getProperty("user.dir"), "src")

    @Test
    fun everyServerMessageKeyExistsInEveryLocalizationCatalog() {
        val catalogs = Files.walk(sourceRoot.resolve("commonMain/resources")).use { stream ->
            stream.filter { it.fileName.toString().startsWith("UiMessages_") && it.toString().endsWith(".properties") }
                .toList()
        }
        assertTrue(catalogs.isNotEmpty(), "No localization catalogs found")
        catalogs.forEach { catalogPath ->
            val properties = Properties().apply {
                Files.newInputStream(catalogPath).use(::load)
            }
            val missing = ServerMessageKey.entries.map(ServerMessageKey::value).filterNot(properties::containsKey)
            assertTrue(missing.isEmpty(), "Missing keys in $catalogPath: $missing")
        }
    }

    @Test
    fun sourceDoesNotCatchThrowableDirectlyOrContainTrailingWhitespace() {
        val sourceFiles = Files.walk(sourceRoot).use { stream ->
            stream.filter { it.toString().endsWith(".kt") }.toList()
        }
        sourceFiles.forEach { sourceFile ->
            sourceFile.readLines().forEachIndexed { index, line ->
                assertFalse(
                    Regex("catch\\s*\\(\\s*error\\s*:\\s*Throwable").containsMatchIn(line),
                    "Do not catch Throwable directly: $sourceFile:${index + 1}"
                )
                assertFalse(
                    Regex("[ \\t]+$").containsMatchIn(line),
                    "Trailing whitespace: $sourceFile:${index + 1}"
                )
            }
        }
    }

    @Test
    fun rawShutdownStateMachineIsOnlyUsedInsideItsKernelOwner() {
        val sourceFiles = Files.walk(sourceRoot.resolve("jvmMain/kotlin")).use { stream ->
            stream.filter { it.toString().endsWith(".kt") }.toList()
        }
        sourceFiles.filterNot { it.toString().contains("server\\kernel\\") }.forEach { sourceFile ->
            val source = Files.readString(sourceFile)
            assertFalse(
                source.contains("ShutdownOperation"),
                "Use ShutdownCoordinator instead of the raw shutdown state machine: $sourceFile"
            )
        }
    }

    @Test
    fun lowLevelStateMutexIsOnlyUsedInsideTheKernel() {
        val sourceFiles = Files.walk(sourceRoot.resolve("jvmMain/kotlin")).use { stream ->
            stream.filter { it.toString().endsWith(".kt") }.toList()
        }
        sourceFiles.filterNot { it.toString().contains("server\\kernel\\") }.forEach { sourceFile ->
            val source = Files.readString(sourceFile)
            assertFalse(
                source.contains("StateMutex"),
                "Use a domain lock boundary instead of StateMutex directly: $sourceFile"
            )
        }
    }
}
