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
    fun routesUseApplicationServicePortsInsteadOfPersistencePackages() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/routes"),
        setOf("f95gm.server.persistence.", "f95gm.server.persistence")
    )

    @Test
    fun runtimeServicesDoNotImportPersistenceImplementation() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/application/port/RouteApplicationServices.kt"),
        setOf("f95gm.server.persistence.database", "f95gm.server.persistence.schema", "org.jetbrains.exposed")
    )

    @Test
    fun applicationServicesDoNotImportHttpTypes() {
        val files = listOf(
            sourceRoot.resolve("f95gm/server/application/IdempotencyApplicationService.kt"),
            sourceRoot.resolve("f95gm/server/application/IdempotencyRegistry.kt"),
            sourceRoot.resolve("f95gm/server/application/DefaultRouteApplicationServices.kt")
        )
        files.forEach { file ->
            assertImportsAbsent(file, setOf("f95gm.server.http", "io.ktor.server", "ApplicationCall", "HttpStatusCode"))
        }
    }

    @Test
    fun persistenceDoesNotImportRoutesOrClient() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/persistence"),
        setOf("f95gm.server.routes", "f95gm.client")
    )

    @Test
    fun applicationAndRouteLayersDoNotConstructUpstreamRequestsDirectly() {
        listOf(
            sourceRoot.resolve("f95gm/server/routes"),
            sourceRoot.resolve("f95gm/server/state/runtime")
        ).forEach { root ->
            assertImportsAbsent(root, setOf("f95gm.server.upstream.request.UpstreamTransport", "io.ktor.client.request"))
            assertSourceAbsent(root, setOf("UpstreamRequest(", "UpstreamTransport("))
        }
    }

    @Test
    fun routesDoNotConstructInfrastructureClientsOrDatabases() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/routes"),
        setOf(
            "io.ktor.client", "org.jetbrains.exposed", "java.sql",
            "f95gm.server.persistence.MarkRepository", "f95gm.server.persistence.SavedFilterRepository"
        )
    )

    @Test
    fun routesDependOnTheUpstreamRequesterPortNotTheConcreteGateway() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/routes"),
        setOf("f95gm.server.upstream.request.UpstreamGateway")
    )

    @Test
    fun persistenceFeaturesDoNotReachAggregateRepositoryFacades() {
        listOf("filters", "marks").forEach { feature ->
            assertImportsAbsent(
                sourceRoot.resolve("f95gm/server/persistence/$feature"),
                setOf(
                    "f95gm.server.persistence.ServerRepositories",
                    "f95gm.server.persistence.MarkRepository",
                    "f95gm.server.persistence.SavedFilterRepository"
                )
            )
        }
    }

    @Test
    fun domainRemainsPlatformAndServerIndependent() = assertImportsAbsent(
        Path.of(System.getProperty("user.dir"), "src", "commonMain", "kotlin", "f95gm", "domain"),
        setOf("f95gm.server", "io.ktor", "java.", "kotlinx.coroutines")
    )

    @Test
    fun configurationDoesNotDependOnRoutes() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/config"),
        setOf("f95gm.server.routes", "io.ktor.server")
    )

    @Test
    fun desktopDoesNotDependOnApplicationComposition() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/desktop"),
        setOf("f95gm.server.app", "f95gm.server.application")
    )

    @Test
    fun routesDoNotImportPersistenceRecordTypes() = assertImportsAbsent(
        sourceRoot.resolve("f95gm/server/routes"),
        setOf(
            "f95gm.server.persistence.model.PersistenceRecords",
            "f95gm.server.persistence.model.MarkRecord",
            "f95gm.server.persistence.model.SavedFilterRecord",
            "f95gm.server.persistence.model.SavedFilterOptionRecord"
        )
    )

    @Test
    fun clientActionsUseApiAndStateBoundaries() = assertImportsAbsent(
        Path.of(System.getProperty("user.dir"), "src", "jsMain", "kotlin", "f95gm/client/actions"),
        setOf("f95gm.client.state.runtime.http", "io.ktor.client.request")
    )

    @Test
    fun routeAndApplicationLayersDoNotBypassRepositoryPorts() {
        assertImportsAbsent(
            sourceRoot.resolve("f95gm/server/routes"),
            setOf("Exposed", "org.jetbrains.exposed", "ExposedSessionRepository", "Database.connect")
        )
        listOf(
            sourceRoot.resolve("f95gm/server/application/port/RouteApplicationServices.kt"),
            sourceRoot.resolve("f95gm/server/application/DefaultRouteApplicationServices.kt")
        ).forEach { file ->
            assertImportsAbsent(file, setOf("org.jetbrains.exposed", "ExposedSessionRepository", "Database.connect"))
        }
    }

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

    private fun assertSourceAbsent(root: Path, forbidden: Set<String>) {
        val files = if (root.isDirectory()) {
            Files.walk(root).use { stream -> stream.filter { it.toString().endsWith(".kt") }.toList() }
        } else {
            listOf(root)
        }
        files.forEach { file ->
            val source = file.readText()
            forbidden.forEach { token ->
                assertFalse(source.contains(token), "Forbidden source '$token' in $file")
            }
        }
    }
}
