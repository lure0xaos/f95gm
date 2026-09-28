package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyOpenApiSnapshotTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baseline: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generated: RegularFileProperty

    @TaskAction
    fun verify() {
        OpenApiContractChecks.assertCompatible(
            OpenApiContractChecks.read(baseline.get().asFile, "OpenAPI baseline"),
            OpenApiContractChecks.read(generated.get().asFile, "generated OpenAPI")
        )
    }
}

abstract class VerifyApiContractCompatibilityTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val baseline: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val generated: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun verify() {
        val before = OpenApiContractChecks.read(baseline.get().asFile, "OpenAPI baseline")
        check(before.containsKey("paths") && before.containsKey("components")) {
            "Previous-release baseline must be a complete OpenAPI snapshot"
        }
        OpenApiContractChecks.assertCompatible(before, OpenApiContractChecks.read(generated.get().asFile, "generated OpenAPI"))
    }
}

abstract class VerifyApiDeprecationsTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val generated: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun verify() = OpenApiContractChecks.assertDeprecations(
        OpenApiContractChecks.read(generated.get().asFile, "generated OpenAPI")
    )
}

abstract class GenerateApiContractChangelogTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val baseline: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val generated: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.OutputFile
    abstract val output: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                OpenApiContractChecks.changelog(
                    OpenApiContractChecks.read(baseline.get().asFile, "OpenAPI baseline"),
                    OpenApiContractChecks.read(generated.get().asFile, "generated OpenAPI")
                )
            )
        }
    }
}

abstract class VerifyReproducibleGeneratedArtifactsTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val generatedContract: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val repeatContract: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val generatedClient: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val repeatClient: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun verify() {
        val contract = generatedContract.get().asFile.readText()
        val repeatContractText = repeatContract.get().asFile.readText()
        val client = generatedClient.get().asFile.readText()
        val repeatClientText = repeatClient.get().asFile.readText()
        check(contract == repeatContractText) { "Repeated isolated OpenAPI generation produced different bytes" }
        check(client == repeatClientText) { "Repeated isolated frontend manifest generation produced different bytes" }
        check("generatedAt" !in contract && "timestamp" !in contract.lowercase()) {
            "OpenAPI output contains unstable build metadata"
        }
        check("System.currentTimeMillis" !in client && "UUID.randomUUID" !in client) {
            "Generated frontend manifest contains unstable build data"
        }
    }
}


