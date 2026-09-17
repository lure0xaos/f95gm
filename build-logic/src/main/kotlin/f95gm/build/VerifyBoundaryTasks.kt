package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyApiBoundaryTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val violations = sourceFiles.files.flatMap { file ->
            SourceBoundaryChecks.apiViolations(file.invariantSeparatorsPath, file.readText())
        }
        check(violations.isEmpty()) { "API transport must stay behind the client transport boundary: $violations" }
    }
}

abstract class VerifyPrimitiveBoundaryTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val primitiveTimestamp = Regex("\\b(updatedAt|createdAt|expiresAt)\\s*:\\s*Long\\b")
        val violations = sourceFiles.files.flatMap { file ->
            val text = file.readText()
            val code = kotlinCode(text)
            primitiveTimestamp.findAll(code).map { match ->
                "${file.invariantSeparatorsPath}:${text.lineNumberAt(match.range.first)}"
            }.toList()
        }
        check(violations.isEmpty()) {
            "Persisted/domain timestamps must use a typed epoch value: $violations"
        }
    }
}

/** Keeps untyped JSON and raw transport values inside explicitly named adapters. */
abstract class VerifyTypedBoundaryTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val violations = sourceFiles.files.flatMap { file ->
            SourceBoundaryChecks.typedViolations(file.invariantSeparatorsPath, file.readText())
        }
        check(violations.isEmpty()) { "Typed boundary violations: $violations" }
    }
}

/** Keeps structured-log field construction on the typed schema vocabulary. */
abstract class VerifyTypedObservabilityTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val violations = sourceFiles.files.flatMap { file ->
            val text = file.readText()
            val typedRegistry = sourcePolicyPath(file.invariantSeparatorsPath) ==
                "src/jvmMain/kotlin/f95gm/server/observability/StructuredLogKeys.kt"
            kotlinCodeRegions(
                text,
                Regex("\\blogField\\s*|\\bstructuredFields\\s*|\\bStructuredFieldName\\s*")
            ).distinctBy { it.start }.mapNotNull { region ->
                val call = text.substring(region.start, region.endExclusive)
                val rawField = Regex("\\blogField\\s*\\(\\s*\"").containsMatchIn(call)
                val rawBuilderField = Regex("\\btext\\s*\\(\\s*\"").containsMatchIn(call)
                val rawName = Regex("\\bStructuredFieldName\\s*\\(\\s*\"").containsMatchIn(call)
                if (rawField || rawBuilderField || rawName && !typedRegistry) {
                    file.invariantSeparatorsPath + ":" + text.lineNumberAt(region.start)
                } else null
            }
        }
        check(violations.isEmpty()) {
            "Structured logs must use the registered typed StructuredField vocabulary: $violations"
        }
    }
}

abstract class VerifySensitiveLoggingTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val sensitiveNames = Regex("\\b(password|token|cookie|authorization|sessionId|accountKey)\\b", RegexOption.IGNORE_CASE)
        val violations = sourceFiles.files.flatMap { file ->
            val text = file.readText()
            kotlinCodeRegions(text, Regex("\\blogger\\s*\\.\\s*[A-Za-z_][A-Za-z0-9_]*|\\bstructuredLog"))
                .distinctBy { it.start }
                .mapNotNull { region ->
                    val call = text.substring(region.start, region.endExclusive)
                    if (sensitiveNames.containsMatchIn(call) && !call.contains("redactLogValue")) {
                        "${file.invariantSeparatorsPath}:${text.lineNumberAt(region.start)}"
                    } else null
                }
        }
        check(violations.isEmpty()) {
            "Sensitive values must be redacted before logging: $violations"
        }
    }
}

private fun String.lineNumberAt(offset: Int): Int =
    substring(0, offset.coerceIn(0, length)).count { it == '\n' } + 1

abstract class VerifyClientGlobalUsageTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val maximumUsages: Property<Int>

    @TaskAction
    fun verify() {
        val deprecatedImports = setOf(
            "f95gm.client.state.runtime.clientState",
            "f95gm.client.state.runtime.clientController",
            "f95gm.client.state.runtime.scope",
            "f95gm.client.state.runtime.clientStoreJob"
        )
        val usages = sourceFiles.files.sumOf { file ->
            kotlinImports(file.readText()).count { it in deprecatedImports }
        }
        check(usages <= maximumUsages.get()) {
            "Deprecated client-global usage increased to $usages; budget is ${maximumUsages.get()}"
        }
    }
}

/** Dependency-only verification task used for lifecycle checks that have no custom action. */
abstract class VerificationTask : DefaultTask()

abstract class VerifyExceptionBoundariesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val violations = sourceFiles.files.flatMap { file ->
            BoundarySourcePolicy.broadCatchViolations(file.invariantSeparatorsPath, file.readText())
        }
        check(violations.isEmpty()) {
            "Broad exception catches require an explicit infrastructure boundary: $violations"
        }
    }
}

abstract class VerifyServiceFactoryUsageTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val allowed = setOf(
            "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerRuntimeServices.kt",
            "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerRuntimeAssembly.kt",
            "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerServiceFactoryModules.kt",
            "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerResources.kt",
            "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerPersistenceServices.kt",
            "src/jvmMain/kotlin/f95gm/server/state/runtime/ServerUpstreamServices.kt"
        )
        val serviceConstructors = Regex("\\b(ServerResources|ServerPersistenceServices|ServerUpstreamServices)\\s*\\(")
        val violations = sourceFiles.files.flatMap { file ->
            if (sourcePolicyPath(file.invariantSeparatorsPath) in allowed) emptyList() else kotlinCode(file.readText()).lines().mapIndexedNotNull { index, line ->
                if (serviceConstructors.containsMatchIn(line)) "${file.name}:${index + 1}" else null
            }
        }
        check(violations.isEmpty()) { "Infrastructure services must be constructed through factories: $violations" }
    }
}

abstract class VerifyRouteApplicationDependenciesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val forbidden = listOf(
            "import f95gm.server.app.",
            "import f95gm.server.state.runtime.ServerRuntimeServices",
            "routeDependencies"
        )
        val serviceDefinitions = setOf(
            "src/jvmMain/kotlin/f95gm/server/routes/session/LoginApplicationService.kt",
            "src/jvmMain/kotlin/f95gm/server/routes/catalog/CatalogApplicationService.kt",
            "src/jvmMain/kotlin/f95gm/server/routes/catalog/CatalogOptionsApplicationService.kt",
            "src/jvmMain/kotlin/f95gm/server/routes/item/ItemApplicationService.kt",
            "src/jvmMain/kotlin/f95gm/server/routes/item/MediaApplicationService.kt"
        )
        val constructedServices = Regex("\\b(LoginApplicationService|CatalogApplicationService|CatalogOptionsApplicationService|ItemApplicationService|MediaApplicationService)\\s*\\(")
        val violations = sourceFiles.files.flatMap { file ->
            kotlinCode(file.readText()).lines().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                if (forbidden.any(trimmed::startsWith) ||
                    (sourcePolicyPath(file.invariantSeparatorsPath) !in serviceDefinitions && constructedServices.containsMatchIn(trimmed) &&
                        !trimmed.startsWith("internal class ") && !trimmed.startsWith("class "))
                ) "${file.name}:${index + 1}: $trimmed" else null
            }
        }
        check(violations.isEmpty()) { "Route code depends directly on application/runtime services: $violations" }
    }
}

abstract class VerifyApplicationPortPurityTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val forbiddenPrefixes = listOf(
            "f95gm.server.application.",
            "f95gm.server.persistence.",
            "f95gm.server.routes.",
            "io.ktor."
        )
        val violations = sourceFiles.files.flatMap { file ->
            val imports = kotlinImports(file.readText())
            imports.filter { import -> forbiddenPrefixes.any(import::startsWith) }
                .map { import -> "${file.name}: import $import" }
        }
        check(violations.isEmpty()) { "Application ports must not import concrete application, persistence, route, or HTTP types: $violations" }
    }
}

abstract class VerifySystemPropertyAccessTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val access = Regex("\\bSystem\\.(getProperty|getProperties|getenv)\\s*\\(")
        val violations = sourceFiles.files.flatMap { file ->
            if (file.invariantSeparatorsPath.endsWith("/server/platform/SystemConfigurationSource.kt")) emptyList()
            else kotlinCode(file.readText()).lines().mapIndexedNotNull { index, line ->
                if (access.containsMatchIn(line)) "${file.name}:${index + 1}" else null
            }
        }
        check(violations.isEmpty()) { "System properties/environment must be accessed through SystemConfigurationSource: $violations" }
    }
}

abstract class VerifyClockAccessTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val directClockAccess = Regex("\\bSystem\\.(currentTimeMillis|nanoTime)\\s*\\(")
        val allowed = setOf("/server/platform/SystemServerClock.kt", "/server/kernel/ServerClock.kt")
        val violations = sourceFiles.files.flatMap { file ->
            if (allowed.any(file.invariantSeparatorsPath::endsWith)) emptyList()
            else kotlinCode(file.readText()).lines().mapIndexedNotNull { index, line ->
                if (directClockAccess.containsMatchIn(line)) "${file.invariantSeparatorsPath}:${index + 1}" else null
            }
        }
        check(violations.isEmpty()) {
            "Direct system clock access must go through ServerClock: $violations"
        }
    }
}
