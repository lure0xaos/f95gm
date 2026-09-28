package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

private data class ImportRule(
    val appliesTo: (String) -> Boolean,
    val forbiddenImports: Set<String>
)

private data class KotlinSourceModel(val file: java.io.File, val imports: Set<String>)

private fun java.io.File.sourceModel(): KotlinSourceModel = KotlinSourceModel(
    this,
    kotlinImports(readText())
)

private fun String.lineNumberAt(offset: Int): Int =
    substring(0, offset.coerceIn(0, length)).count { it == '\n' } + 1

private data class RouteOwnershipRule(val allowedDependencies: Set<String>)

private val routeOwnershipRule = RouteOwnershipRule(setOf(
    "LoginRouteDependencies", "CatalogRouteDependencies", "ItemRouteDependencies",
    "FilterRouteDependencies", "MarkRouteDependencies", "MediaRouteDependencies",
    "SessionRouteDependencies", "ApplicationRouteDependencies", "ClockRouteDependencies",
    "LiveMarkRouteDependencies", "UpstreamRouteDependencies"
))

private fun importViolations(files: Set<java.io.File>, rules: List<ImportRule>): List<String> = files.map { it.sourceModel() }.flatMap { model ->
    val file = model.file
    val relative = file.invariantSeparatorsPath
    val forbidden = rules.filter { it.appliesTo(relative) }.flatMap { it.forbiddenImports }
    model.imports.mapNotNull { import ->
        if (forbidden.any(import::startsWith)) "${file.name}: $import" else null
    }
}

abstract class VerifyServerDependencyGraphTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val violations = importViolations(sourceFiles.files, listOf(
            ImportRule({ it.contains("server/routes/") }, setOf(
                    "f95gm.server.state.runtime.ServerRuntimeServices",
                    "f95gm.server.state.runtime.ServerResources",
                    "f95gm.server.state.runtime.RequestMonitoringInfrastructureServices",
                    "f95gm.server.persistence",
                    "io.ktor.client",
                    "f95gm.server.state.runtime.cache",
                    "f95gm.server.http.static.StaticResourceCache",
                    "f95gm.server.upstream.request.UpstreamHttpClient",
                    "f95gm.server.upstream.request.UpstreamGateway"
                )),
            ImportRule({ path ->
                path.contains("server/app/") &&
                    !path.endsWith("StartServer.kt") &&
                    !path.endsWith("ServerCompositionRoot.kt") &&
                    !path.endsWith("GenerateMigrationPlan.kt")
            }, setOf("f95gm.server.persistence.database", "org.jetbrains.exposed", "io.ktor.client.request")),
            ImportRule({ it.contains("server/persistence/") }, setOf("f95gm.server.routes", "f95gm.client")),
            ImportRule({ path ->
                path.contains("server/persistence/filters/") || path.contains("server/persistence/marks/")
            }, setOf(
                "f95gm.server.persistence.MarkRepository",
                "f95gm.server.persistence.SavedFilterRepository",
                "f95gm.server.persistence.ServerRepositories",
                "f95gm.server.persistence.MarkRecord",
                "f95gm.server.persistence.SavedFilterRecord",
                "f95gm.server.persistence.requireStorageLength"
            )),
            ImportRule({ it.contains("server/config/") }, setOf("f95gm.server.routes", "f95gm.server.desktop")),
            ImportRule({ it.contains("server/desktop/") }, setOf("f95gm.server.routes", "f95gm.server.persistence")),
            ImportRule({ it.contains("server/upstream/") }, setOf("f95gm.server.routes", "f95gm.server.persistence")),
            ImportRule({ it.contains("server/http/") }, setOf("f95gm.server.desktop")),
            ImportRule({ it.contains("server/platform/") }, setOf("f95gm.server.launch"))
        ))
        check(violations.isEmpty()) { "Route dependency graph bypasses declared ports: $violations" }
        val compatibilityConstantUsages = sourceFiles.files.map { it.sourceModel() }.flatMap { model ->
            val file = model.file
            val text = file.readText()
            Regex("\\bServerConstants\\s*\\.").findAll(kotlinCode(text))
                .map { file.name + ":" + text.lineNumberAt(it.range.first) }
                .toList()
        }
        check(compatibilityConstantUsages.isEmpty()) {
            "Production code must use focused protocol, storage, and policy constants: $compatibilityConstantUsages"
        }
    }
}

abstract class VerifyRouteDependencyOwnershipTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val violations = sourceFiles.files.map { it.sourceModel() }.flatMap { model ->
            val file = model.file
            file.readLines().mapIndexedNotNull { index, line ->
                val import = line.trim().removePrefix("import ")
                val dependency = import.substringAfterLast('.', "")
                val bypassesOperationalPorts = import == "f95gm.server.config.RequestLimits" ||
                    import == "f95gm.server.config.MediaLimits" ||
                    import == "f95gm.server.config.CatalogLimits" ||
                    import.startsWith("f95gm.server.config.constants.") && import.endsWith("Limits")
                if (bypassesOperationalPorts ||
                    ((import.contains("f95gm.server.state.runtime") || import.contains("f95gm.server.routes")) &&
                        dependency.endsWith("RouteDependencies") && dependency !in routeOwnershipRule.allowedDependencies)) {
                    "${file.name}:${index + 1}: $import"
                } else null
            }
        }
        check(violations.isEmpty()) { "Route code imports an unowned dependency port: $violations" }
    }
}

abstract class VerifyCommonJvmSeparationTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val forbidden = setOf("java.", "javax.", "io.ktor.", "kotlinx.coroutines.")
        val violations = sourceFiles.files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val import = line.trim().removePrefix("import ")
                if (forbidden.any(import::startsWith)) "${file.name}:${index + 1}: $import" else null
            }
        }
        check(violations.isEmpty()) { "Common source imports JVM/platform infrastructure: $violations" }
    }
}
