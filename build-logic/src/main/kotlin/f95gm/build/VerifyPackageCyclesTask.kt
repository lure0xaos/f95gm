package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFile

abstract class VerifyPackageCyclesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:InputFile
    abstract val baseline: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val packageByFile = sourceFiles.files.associateWith { file ->
            file.readLines().firstOrNull { it.startsWith("package ") }
                ?.removePrefix("package ")?.trim()
                ?: error("Missing package declaration in $file")
        }
        val packages = packageByFile.values.toSet()
        val graph = packages.associateWith { pkg ->
            packageByFile.filterValues { it == pkg }.keys
                .flatMap { file ->
                    kotlinImports(file.readText())
                        .filter { it.startsWith("f95gm.server.") }
                        .map { imported -> imported.substringBeforeLast('.', missingDelimiterValue = "") }
                }
                .filter { it in packages && it != pkg }
                .toSet()
        }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val cycles = mutableListOf<String>()

        fun visit(pkg: String, path: List<String>) {
            if (pkg in visiting) {
                cycles += (path + pkg).joinToString(" -> ")
                return
            }
            if (!visited.add(pkg)) return
            visiting += pkg
            graph[pkg].orEmpty().forEach { visit(it, path + pkg) }
            visiting -= pkg
        }

        packages.forEach { visit(it, emptyList()) }
        val markers = baseline.get().asFile.readLines().map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        check(markers.distinct().size == markers.size) {
            "Package-cycle baseline contains duplicate markers; keep debt entries unique"
        }
        val staleMarkers = markers.filter { marker -> cycles.none { cycle -> cycle.contains(marker) } }
        check(staleMarkers.isEmpty()) {
            "Package-cycle baseline contains stale markers; remove them as debt is eliminated: $staleMarkers"
        }
        val newCycles = cycles.distinct().filterNot { cycle -> markers.any(cycle::contains) }
        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(buildString {
            appendLine("# Package cycle debt report")
            appendLine("baseline=${markers.joinToString(",")}")
            appendLine("cycles=${cycles.distinct().size}")
            appendLine("tracked=${cycles.distinct().count { cycle -> markers.any(cycle::contains) }}")
            appendLine("untracked=${newCycles.size}")
            cycles.distinct().forEach { appendLine("cycle=$it") }
        })
        check(newCycles.isEmpty()) {
            "Package dependency cycles outside the approved baseline detected: $newCycles"
        }
    }
}
