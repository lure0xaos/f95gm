package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyTypedClientArchitectureTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val forbidden = listOf(
            Regex("\\bApiException\\s*\\(\\s*\\\""),
            Regex("\\boverride\\s+fun\\s+response\\s*\\(\\s*path\\s*:\\s*String"),
            Regex("\\boverride\\s+fun\\s+transportUnavailable\\s*\\("),
            Regex("\\bnormalizeApiException\\s*\\([^)]*\\bpath\\s*=\\s*\\\"", RegexOption.DOT_MATCHES_ALL),
            Regex("\\bPayloadParseException\\s*\\([^)]*\\bendpoint\\s*=\\s*\\\"", RegexOption.DOT_MATCHES_ALL),
        )
        val violations = sourceFiles.files.flatMap { file ->
            val path = file.invariantSeparatorsPath
            val isCompositionRoot = path.endsWith("client/state/runtime/ClientState.kt") ||
                path.endsWith("client/state/runtime/BrowserEffects.kt") ||
                path.endsWith("client/routing/navigation/Router.kt") ||
                path.endsWith("client/bootstrap/ClientComposition.kt")
            val text = file.readText()
            val code = kotlinCode(text)
            val matches = forbidden.flatMap { pattern ->
                pattern.findAll(code).map { match -> text.lineNumberAt(match.range.first) }
            }
            val globalMatches = if (isCompositionRoot) emptyList() else
                Regex("\\bWindowBrowserEffects\\b").findAll(code).map { match -> text.lineNumberAt(match.range.first) }.toList()
            val aggregateRuntimeMatches = if (isCompositionRoot || path.contains("client/state/runtime/")) emptyList() else
                Regex("\\bClientRuntime\\b").findAll(code).map { match -> text.lineNumberAt(match.range.first) }.toList()
            val isFeatureConsumer = path.contains("client/actions/") || path.contains("client/ui/")
            val aggregateStateMatches = if (isFeatureConsumer)
                Regex("\\bClientState\\b|\\bruntime\\.state\\b").findAll(code)
                    .map { match -> text.lineNumberAt(match.range.first) }.toList()
                else emptyList()
            val clientStateOwners = path.endsWith("client/state/runtime/ClientState.kt") ||
                path.endsWith("client/bootstrap/ClientComposition.kt") ||
                path.endsWith("client/bootstrap/StartClient.kt") ||
                path.endsWith("client/entry/Main.kt")
            val concreteStateMatches = if (clientStateOwners) emptyList() else
                Regex("\\bClientState\\b").findAll(code)
                    .map { match -> text.lineNumberAt(match.range.first) }.toList()
            val aggregateUiMatches = if (path.contains("client/ui/") && !path.endsWith("pages/AuthenticatedPage.kt"))
                Regex("\\bClientUiPort\\b").findAll(code).map { match -> text.lineNumberAt(match.range.first) }.toList()
                else emptyList()
            val uiStoreWrites = if (path.contains("client/ui/"))
                Regex("\\.enqueue\\s*\\(").findAll(code)
                    .map { match -> text.lineNumberAt(match.range.first) }.toList()
                else emptyList()
            (matches + globalMatches + aggregateRuntimeMatches + aggregateStateMatches +
                concreteStateMatches + aggregateUiMatches + uiStoreWrites)
                .distinct().map { line -> "${file.name}:$line" } +
                BoundarySourcePolicy.uiCapabilityViolations(path, text) +
                SourceBoundaryChecks.controllerViolations(path, text)
        }
        check(violations.isEmpty()) { "Legacy typed-boundary usage detected: $violations" }
    }
}

private fun String.lineNumberAt(offset: Int): Int =
    substring(0, offset.coerceIn(0, length)).count { it == '\n' } + 1
