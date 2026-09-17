package f95gm.build

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

internal object OpenApiContractChecks {
    fun read(file: File, context: String): Map<*, *> = OpenApiJson.parse(file.readText(), context)

    fun assertCompatible(baseline: Map<*, *>, generated: Map<*, *>) {
        val before = OpenApiContractModel.parse(baseline)
        val after = OpenApiContractModel.parse(generated)
        check(before.openApi == after.openApi) { "OpenAPI version changed incompatibly" }
        val beforeOperations = before.operations.associateBy { it.path to it.method }
        val afterOperations = after.operations.associateBy { it.path to it.method }
        check(afterOperations.keys.containsAll(beforeOperations.keys)) { "Generated API removed operations" }
        beforeOperations.forEach { (key, oldOperation) ->
            val newOperation = afterOperations.getValue(key)
            check(newOperation.operationId == oldOperation.operationId) { "Operation ID changed for ${key.second} ${key.first}" }
            check(newOperation.metadata == oldOperation.metadata) { "Contract metadata changed for ${key.second} ${key.first}" }
            check(newOperation.parameters.containsAll(oldOperation.parameters)) { "Parameters were removed from ${key.second} ${key.first}" }
            check(newOperation.responses.keys.containsAll(oldOperation.responses.keys)) { "Responses were removed from ${key.second} ${key.first}" }
            oldOperation.responses.forEach { (status, mediaTypes) ->
                check(newOperation.responses.getValue(status).containsAll(mediaTypes)) {
                    "Response media types changed for $status ${key.second} ${key.first}"
                }
            }
        }
        check(after.schemas.keys.containsAll(before.schemas.keys)) { "Generated API removed schemas" }
        before.schemas.forEach { (name, oldSchema) ->
            val newSchema = after.schemas.getValue(name)
            assertSchemaNodeCompatible(oldSchema.node, newSchema.node, "schema $name")
            check(newSchema.type == oldSchema.type)
            check(newSchema.constraints == oldSchema.constraints) { "Schema constraints changed for $name" }
            check(newSchema.enumValues.orEmpty().containsAll(oldSchema.enumValues.orEmpty())) {
                "Enum values were removed from schema $name"
            }
            check(newSchema.required.containsAll(oldSchema.required)) {
                "Required fields were removed from schema $name"
            }
            check(newSchema.properties.keys.containsAll(oldSchema.properties.keys)) {
                "Properties were removed from schema $name"
            }
            oldSchema.properties.forEach { (propertyName, oldProperty) ->
                val newProperty = newSchema.properties.getValue(propertyName)
                check(newProperty.type == oldProperty.type && newProperty.format == oldProperty.format &&
                    newProperty.reference == oldProperty.reference) {
                    "Property $name.$propertyName changed type"
                }
                check(newProperty.constraints == oldProperty.constraints) {
                    "Property constraints changed for $name.$propertyName"
                }
                check(newProperty.enumValues.orEmpty().containsAll(oldProperty.enumValues.orEmpty())) {
                    "Enum values were removed from $name.$propertyName"
                }
            }
        }
    }

    private fun assertSchemaNodeCompatible(
        before: OpenApiSchemaNode,
        after: OpenApiSchemaNode,
        context: String
    ) {
        check(before.type == after.type && before.format == after.format && before.reference == after.reference) {
            "$context changed type, format, or reference"
        }
        check(before.constraints == after.constraints) { "$context changed schema constraints" }
        check(after.enumValues.orEmpty().containsAll(before.enumValues.orEmpty())) {
            "$context removed enum values"
        }
        check(after.required.containsAll(before.required)) { "$context removed required fields" }
        check(after.properties.keys.containsAll(before.properties.keys)) { "$context removed properties" }
        before.properties.forEach { (name, oldProperty) ->
            assertSchemaNodeCompatible(oldProperty, after.properties.getValue(name), "$context.$name")
        }
        check((before.items == null) == (after.items == null)) { "$context changed items schema presence" }
        if (before.items != null && after.items != null) {
            assertSchemaNodeCompatible(before.items, after.items, "$context.items")
        }
        check(before.additionalPropertiesAllowed == after.additionalPropertiesAllowed) {
            "$context changed additionalProperties policy"
        }
        check((before.additionalProperties == null) == (after.additionalProperties == null)) {
            "$context changed additionalProperties schema presence"
        }
        if (before.additionalProperties != null && after.additionalProperties != null) {
            assertSchemaNodeCompatible(before.additionalProperties, after.additionalProperties, "$context.additionalProperties")
        }
        check(after.alternatives.keys.containsAll(before.alternatives.keys)) {
            "$context removed schema alternatives"
        }
        before.alternatives.forEach { (kind, oldAlternatives) ->
            val newAlternatives = after.alternatives.getValue(kind)
            check(newAlternatives.size >= oldAlternatives.size) { "$context removed $kind alternatives" }
            oldAlternatives.zip(newAlternatives).forEachIndexed { index, (oldAlternative, newAlternative) ->
                assertSchemaNodeCompatible(oldAlternative, newAlternative, "$context.$kind[$index]")
            }
        }
    }

    fun assertDeprecations(document: Map<*, *>) {
        OpenApiContractModel.parse(document).operations.forEach { operation ->
            if (operation.metadata["deprecated"]?.booleanOrNull == true) {
                val id = operation.operationId ?: "unknown"
                check(operation.metadata["x-f95gm-deprecation-replacement"] != null) {
                    "Deprecated operation $id must declare x-f95gm-deprecation-replacement"
                }
                check(operation.metadata["x-f95gm-deprecation-sunset"] != null) {
                    "Deprecated operation $id must declare x-f95gm-deprecation-sunset"
                }
            }
        }
    }

    fun changelog(baseline: Map<*, *>, generated: Map<*, *>): String {
        val before = OpenApiContractModel.parse(baseline).operations.associateBy { it.path to it.method }
        val after = OpenApiContractModel.parse(generated).operations.associateBy { it.path to it.method }
        val beforeKeys = before.keys.map { "${it.first} ${it.second}" }.toSet()
        val afterKeys = after.keys.map { "${it.first} ${it.second}" }.toSet()
        val added = (afterKeys - beforeKeys).sorted()
        val removed = (beforeKeys - afterKeys).sorted()
        val changed = beforeKeys.intersect(afterKeys).filter { key ->
            val split = key.lastIndexOf(' ')
            val path = key.substring(0, split)
            val method = key.substring(split + 1)
            before.getValue(path to method).operationId != after.getValue(path to method).operationId
        }.sorted()
        return buildString {
            appendLine("# API contract changes")
            appendLine()
            appendLine("Generated from the previous-release and current OpenAPI snapshots.")
            appendLine()
            appendSection("Added operations", added)
            appendSection("Removed operations", removed)
            appendSection("Operation identity changes", changed)
        }
    }

    private fun StringBuilder.appendSection(title: String, values: List<String>) {
        appendLine("## $title")
        if (values.isEmpty()) appendLine("- None") else values.forEach { appendLine("- `$it`") }
        appendLine()
    }

}
