package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Generates the server-side operation inventory from the same contract as the frontend client. */
abstract class GenerateServerApiRegistryTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contract: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generate() {
        val operations = OpenApiContractModel.operations(contract.get().asFile.readText())
            .map { it.operationId ?: error("Missing operationId for ${it.method} ${it.path}") }
            .sorted()
        require(operations.distinct().size == operations.size) {
            "OpenAPI operationIds must be unique: ${operations.groupingBy { it }.eachCount().filterValues { it > 1 }.keys}"
        }
        output.get().asFile.apply {
            parentFile.mkdirs()
            fun String.asKotlinString(): String = buildString {
                append('"')
                for (character in this@asKotlinString) {
                    when (character) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '$' -> append("\\$")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(character)
                    }
                }
                append('"')
            }
            writeText(buildString {
            appendLine("package f95gm.server.app")
            appendLine()
            appendLine("import f95gm.domain.api.ApiMethod")
            appendLine("import f95gm.domain.api.ApiOperationId")
            appendLine("import f95gm.domain.api.ApiPath")
            appendLine()
            appendLine("/** Generated from the checked OpenAPI operation registry. */")
            appendLine("internal data class GeneratedServerApiOperation(")
            appendLine("    val id: ApiOperationId,")
            appendLine("    val method: ApiMethod,")
            appendLine("    val path: ApiPath")
            appendLine(")")
            appendLine()
            appendLine("internal object GeneratedServerApiRegistry {")
            appendLine("    val operations: List<GeneratedServerApiOperation> = listOf(")
            OpenApiContractModel.operations(contract.get().asFile.readText()).sortedBy { it.operationId }.forEach { operation ->
                val id = operation.operationId ?: error("Missing operationId for ${operation.method} ${operation.path}")
                appendLine("        GeneratedServerApiOperation(ApiOperationId(${id.asKotlinString()}), ApiMethod.${operation.method}, ApiPath(${operation.path.asKotlinString()})),")
            }
            appendLine("    )")
            appendLine("    val byId: Map<ApiOperationId, GeneratedServerApiOperation> = operations.associateBy { it.id }")
            appendLine("    private val byMethodAndPath = operations.associateBy { it.method to it.path }")
            appendLine("    fun require(id: ApiOperationId): GeneratedServerApiOperation = byId.getValue(id)")
            appendLine("    fun require(method: ApiMethod, path: ApiPath): GeneratedServerApiOperation =")
            appendLine("        byMethodAndPath.getValue(method to path)")
            appendLine("    val operationIds: Set<ApiOperationId> = operations.map { it.id }.toSet()")
            appendLine("}")
            })
        }
    }
}
