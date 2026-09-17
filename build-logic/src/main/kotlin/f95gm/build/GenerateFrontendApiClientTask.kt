package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateFrontendApiClientTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contract: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generate() {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(frontendManifest(contract.get().asFile.readText()))
        }
    }

    private data class GeneratedOperation(
        val id: String,
        val method: String,
        val path: String,
        val requestSchema: String?,
        val responseSchema: String?,
        val responseSchemas: Set<String>
    )

    private fun generatedOperations(contractJson: String): List<GeneratedOperation> =
        OpenApiContractModel.operations(contractJson).map { operation ->
            GeneratedOperation(
                operation.operationId ?: error("Missing operationId for ${operation.method} ${operation.path}"),
                operation.method,
                operation.path,
                operation.requestSchema,
                operation.responseSchema,
                operation.responseSchemas
            )
        }

    private fun frontendManifest(contractJson: String): String {
        val operations = generatedOperations(contractJson)
        val supportedSchemas = setOf(
            "JsonObject", "ApiError", "LoginRequest", "OkResponse", "CatalogOptions", "Marks", "SavedFilters",
            "SavedFilterRequest", "QuickLinksBackup", "ImportedQuickLinks", "MarkRequest", "ThreadDetail", "DetailMeta",
            "DetailSection", "SavedFilter", "SavedFilterOption", "MarkResponse", "RequestMetrics", "EmptyRequest",
            "UpstreamCatalog", "Media"
        )
        operations.flatMap { listOfNotNull(it.requestSchema, it.responseSchema) + it.responseSchemas }.forEach { schema ->
            check(schema in supportedSchemas) {
                "OpenAPI schema '$schema' is not represented by ApiSchema"
            }
        }
        check(operations.map(GeneratedOperation::id).distinct().size == operations.size) {
            "OpenAPI operation IDs must be unique"
        }
        check(operations.all { it.id.isNotBlank() }) { "OpenAPI operation IDs must not be blank" }
        check(operations.all { it.path.startsWith("/") && it.path.isNotBlank() }) {
            "OpenAPI operation paths must be absolute path templates"
        }
        check(operations.map { it.method to it.path }.distinct().size == operations.size) {
            "OpenAPI method/path pairs must be unique"
        }
        fun String.asIdentifier(): String {
            val identifier = replace(Regex("[^A-Za-z0-9_]"), "_")
            check(identifier.isNotBlank()) { "OpenAPI operation ID must produce a non-blank Kotlin identifier: $this" }
            val kotlinKeywords = setOf(
                "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
                "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
                "typeof", "val", "var", "when", "while", "by", "catch", "constructor", "delegate", "dynamic", "field",
                "file", "finally", "get", "import", "init", "param", "property", "receiver", "set", "setparam", "where",
                "actual", "abstract", "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external",
                "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override",
                "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg"
            )
            return if (identifier.first().isDigit() || identifier in kotlinKeywords) "_$identifier" else identifier
        }
        fun String.asKotlinString(): String = buildString {
            append('"')
            for (character in this@asKotlinString) {
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> {
                        append('\\')
                        append('$')
                    }
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
            append('"')
        }
        val identifiers = operations.map { it.id.asIdentifier() }
        check(identifiers.distinct().size == identifiers.size) {
            "OpenAPI operation IDs must map to unique Kotlin identifiers"
        }
        fun String.toSchema() = "ApiSchema." + replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_").uppercase()
        return buildString {
            appendLine("package f95gm.client.data.api")
            appendLine()
            appendLine("import f95gm.domain.api.ApiPath")
            appendLine("import f95gm.domain.api.ApiMethod")
            appendLine("import f95gm.domain.api.ApiSchema")
            appendLine("import f95gm.domain.api.ApiOperationId")
            appendLine()
            appendLine("internal data class GeneratedF95ApiOperation(")
            appendLine("    val id: ApiOperationId,")
            appendLine("    val method: ApiMethod,")
            appendLine("    val path: ApiPath,")
            appendLine("    val requestSchema: ApiSchema?,")
            appendLine("    val responseSchema: ApiSchema?")
            appendLine(")")
            appendLine()
            appendLine("internal object GeneratedF95ApiOperations {")
            appendLine("    val operations: List<GeneratedF95ApiOperation> = listOf(")
            operations.forEach { operation ->
                appendLine(
                    "        GeneratedF95ApiOperation(ApiOperationId(${operation.id.asKotlinString()}), ApiMethod.${operation.method}, " +
                        "ApiPath(${operation.path.asKotlinString()}), ${operation.requestSchema?.let { "${it.toSchema()}" } ?: "null"}, " +
                        "${operation.responseSchema?.let { "${it.toSchema()}" } ?: "null"}),"
                )
            }
            appendLine("    )")
            appendLine("    val operationIds: Set<ApiOperationId> = operations.map { it.id }.toSet()")
            appendLine("    val byId: Map<ApiOperationId, GeneratedF95ApiOperation> = operations.associateBy { it.id }")
            appendLine("    fun require(id: ApiOperationId): GeneratedF95ApiOperation = byId.getValue(id)")
            operations.forEachIndexed { index, operation ->
                appendLine("    val ${identifiers[index]} = byId.getValue(ApiOperationId(${operation.id.asKotlinString()}))")
            }
            appendLine("}")
        }
    }
}
