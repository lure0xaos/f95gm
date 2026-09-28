package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

abstract class VerifyKotlinStyleTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        sourceFiles.files.forEach { file ->
            val text = file.readText()
            check(text.lineSequence().none { it.endsWith(" ") || it.endsWith("\t") }) {
                "Trailing whitespace found in $file"
            }
            check(!text.contains("print" + "ln(")) { "Use structured logging instead of println in $file" }
        }
    }
}
abstract class GenerateApiFixtureManifestTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fixtureDirectory: DirectoryProperty

    @get:OutputFile
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun generate() {
        val root = fixtureDirectory.get().asFile
        val digest = MessageDigest.getInstance("SHA-256")
        val lines = root.walkTopDown().filter(File::isFile).sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .map { file ->
                digest.reset()
                val hash = digest.digest(file.readBytes()).joinToString("") { byte -> "%02x".format(byte) }
                "${file.relativeTo(root).invariantSeparatorsPath}  $hash"
            }
        manifest.get().asFile.apply {
            parentFile.mkdirs()
            writeText(lines.joinToString("\n") + "\n")
        }
    }
}

abstract class VerifyUpstreamBoundariesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val protectedSources: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        protectedSources.files.forEach { file ->
            val forbidden = listOf("UpstreamRequest(", "UpstreamTransport(", "io.ktor.client.request")
            check(forbidden.none(file.readText()::contains)) {
                "Direct upstream transport construction is forbidden in ${file.name}"
            }
        }
    }
}

abstract class VerifyTypedContractSurfaceTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generated: RegularFileProperty

    @TaskAction
    fun verify() {
        val text = generated.get().asFile.readText()
        check("requestSchema: ApiSchema?" in text && "responseSchema: ApiSchema?" in text) {
            "Generated client operations must retain request and response schema metadata"
        }
        check("import f95gm.domain.api.ApiOperationId" in text &&
            "operationIds: Set<ApiOperationId>" in text &&
            "byId: Map<ApiOperationId, GeneratedF95ApiOperation>" in text) {
            "Generated client operations must expose stable lookup indexes"
        }
    }
}

abstract class VerifyGeneratedFileTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val expected: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val actual: RegularFileProperty

    @TaskAction
    fun verify() {
        check(expected.get().asFile.readBytes().contentEquals(actual.get().asFile.readBytes())) {
            "Generated file differs from ${expected.get().asFile}"
        }
    }
}

abstract class VerifyDirectoryReproducibilityTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val first: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val second: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val firstRoot = first.singleFile
        val secondRoot = second.singleFile
        val firstFiles = firstRoot.walkTopDown().filter(File::isFile).associateBy { it.relativeTo(firstRoot).invariantSeparatorsPath }
        val secondFiles = secondRoot.walkTopDown().filter(File::isFile).associateBy { it.relativeTo(secondRoot).invariantSeparatorsPath }
        check(firstFiles.keys == secondFiles.keys) { "Reproducibility file sets differ" }
        firstFiles.forEach { (relative, file) ->
            check(file.readBytes().contentEquals(secondFiles.getValue(relative).readBytes())) {
                "Reproducibility output differs for $relative"
            }
        }
    }
}

abstract class VerifyFilesPresentTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToCheck: ConfigurableFileCollection

    @TaskAction
    fun verify() = filesToCheck.files.forEach { check(it.isFile) { "Missing required file: $it" } }
}

abstract class VerifyFrontendBudgetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distribution: DirectoryProperty

    @get:Input
    abstract val maxJavaScriptBytes: Property<Long>

    @TaskAction
    fun verify() {
        val javascriptBytes = distribution.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "js" }
            .sumOf { it.length() }
        check(javascriptBytes <= maxJavaScriptBytes.get()) {
            "Production JavaScript bundle exceeds ${maxJavaScriptBytes.get()} bytes: $javascriptBytes bytes"
        }
    }
}

abstract class VerifyDependencyPolicyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionCatalog: RegularFileProperty

    @TaskAction
    fun verify() {
        val dynamicVersionLine = versionCatalog.get().asFile.readLines().firstOrNull { line ->
            line.contains("version =") && Regex(
                """version\s*=\s*[\"'][^\"']*(latest\.release|latest\.integration|SNAPSHOT|\+|[\[\(,<>])""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(line)
        }
        check(dynamicVersionLine == null) {
            "Dynamic or snapshot dependency version detected in the version catalog: $dynamicVersionLine"
        }
    }
}

