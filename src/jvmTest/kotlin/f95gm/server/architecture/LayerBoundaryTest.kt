package f95gm.server.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse

class LayerBoundaryTest {
    private val sourceRoot = Path.of(System.getProperty("user.dir"), "src", "jvmMain", "kotlin")

    @Test
    fun routesDoNotImportPersistenceImplementation() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/routes"),
        setOf("f95gm.server.persistence.database", "f95gm.server.persistence.schema", "org.jetbrains.exposed")
    )

    @Test
    fun runtimeServicesDoNotImportPersistenceImplementation() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/state/runtime/RouteApplicationServices.kt"),
        setOf("f95gm.server.persistence.database", "f95gm.server.persistence.schema", "org.jetbrains.exposed")
    )

    @Test
    fun persistenceDoesNotImportRoutesOrClient() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/persistence"),
        setOf("f95gm.server.routes", "f95gm.client")
    )

    @Test
    fun routesDoNotConstructInfrastructureClientsOrDatabases() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/routes"),
        setOf("io.ktor.client", "org.jetbrains.exposed", "java.sql")
    )

    @Test
    fun clientActionsUseApiAndStateBoundaries() = assertImportsAbsent(
        Path.of(System.getProperty("user.dir"), "src", "jsMain", "kotlin", "f95gm/client/actions"),
        setOf("f95gm.client.state.runtime.http", "io.ktor.client.request")
    )

    private fun assertImportsAbsent(root: Path, forbidden: Set<String>) {
        val files = if (root.isDirectory()) {
            Files.walk(root).use { stream -> stream.filter { it.toString().endsWith(".kt") }.toList() }
        } else {
            listOf(root)
        }
        files.forEach { file ->
            val imports = file.readText().lineSequence()
                .map(String::trim)
                .filter { it.startsWith("import ") }
            forbidden.forEach { prefix ->
                assertFalse(
                    imports.any { it.removePrefix("import ").startsWith(prefix) },
                    "Forbidden import '$prefix' in $file"
                )
            }
        }
    }
}
