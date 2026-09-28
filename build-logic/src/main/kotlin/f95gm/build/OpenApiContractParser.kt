package f95gm.build

object OpenApiContractParser {
    private val supportedMethods = setOf("get", "post", "put", "delete", "patch", "options", "head", "trace")
    private val comparableMetadata = setOf(
        "x-f95gm-mutation", "x-f95gm-rate-limit", "x-f95gm-api-version", "deprecated",
        "x-f95gm-deprecation-replacement", "x-f95gm-deprecation-sunset"
    )
    private val schemaConstraints = setOf(
        "type", "format", "nullable", "pattern", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum",
        "multipleOf", "minLength", "maxLength", "minItems", "maxItems", "uniqueItems", "minProperties",
        "maxProperties", "readOnly", "writeOnly"
    )

    fun parse(contractJson: String): OpenApiDocumentModel = parse(OpenApiJson.parse(contractJson))

    fun parse(document: Map<*, *>): OpenApiDocumentModel {
        val paths = OpenApiJson.memberObject(document, "paths", "OpenAPI")
        val operations = paths.entries.flatMap { (rawPath, rawPathItem) ->
            val path = rawPath.toString()
            OpenApiJson.objectValue(rawPathItem, "path $path").entries.mapNotNull { (rawMethod, rawOperation) ->
                val method = rawMethod.toString().lowercase()
                if (method !in supportedMethods) return@mapNotNull null
                parseOperation(path, method, OpenApiJson.objectValue(rawOperation, "$method $path"))
            }
        }.sortedWith(compareBy(OpenApiOperationModel::path, OpenApiOperationModel::method))
        return OpenApiDocumentModel(document["openapi"]?.toString(), operations, parseSchemas(document))
    }

    fun operations(contractJson: String): List<OpenApiOperationModel> = parse(contractJson).operations

    private fun parseOperation(path: String, method: String, operation: Map<*, *>): OpenApiOperationModel {
        val rawResponses = OpenApiJson.memberObject(operation, "responses", "operation")
        val responses = rawResponses.entries.associate { (status, rawResponse) ->
            val response = OpenApiJson.objectValue(rawResponse, "response $status")
            check(!OpenApiJson.string(response["description"], "response $status description").isNullOrBlank()) {
                "Response $status for $method $path must have a non-blank description"
            }
            val content = OpenApiJson.optionalObject(response["content"], "response $status content")
                ?.keys?.toSet() ?: emptySet()
            status.toString() to content
        }
        operation["security"]?.let { rawSecurity ->
            val security = OpenApiJson.array(rawSecurity, "security metadata for $method $path")
            security.forEach { requirement ->
                val requirementObject = OpenApiJson.objectValue(requirement, "security requirement for $method $path")
                check(requirementObject.keys.all { it is String }) {
                    "Security requirement keys for $method $path must be strings"
                }
                check(requirementObject.values.all { scopes ->
                    (scopes is List<*> && scopes.all { it is String }) ||
                        (scopes is Map<*, *> && scopes.isEmpty())
                }) {
                    "Security requirement scopes for $method $path must be string arrays"
                }
            }
        }
        if (OpenApiJson.boolean(operation["deprecated"], "operation deprecated") == true) {
            check(!OpenApiJson.string(operation["x-f95gm-deprecation-replacement"], "deprecation replacement").isNullOrBlank()) {
                "Deprecated operation $method $path must declare x-f95gm-deprecation-replacement"
            }
            check(!OpenApiJson.string(operation["x-f95gm-deprecation-sunset"], "deprecation sunset").isNullOrBlank()) {
                "Deprecated operation $method $path must declare x-f95gm-deprecation-sunset"
            }
        }
        val responseSchemas = rawResponses.values.flatMap { rawResponse ->
            val response = OpenApiJson.objectValue(rawResponse, "response")
            val content = OpenApiJson.optionalObject(response["content"], "response content") ?: emptyMap()
            content.values.mapNotNull { rawMedia ->
                val media = OpenApiJson.optionalObject(rawMedia, "response media") ?: return@mapNotNull null
                val schema = OpenApiJson.optionalObject(media["schema"], "response schema") ?: return@mapNotNull null
                OpenApiJson.string(schema["\$ref"], "response schema reference")?.substringAfterLast('/')
            }
        }.toSet()
        val successResponse = rawResponses.entries
            .sortedBy { it.key.toString() }
            .firstOrNull { it.key.toString().startsWith("2") }?.value
        return OpenApiOperationModel(
            path = path,
            method = method.uppercase(),
            operationId = OpenApiJson.string(operation["operationId"], "operationId"),
            metadata = comparableMetadata.mapNotNull { key ->
                operation[key]?.let { key to OpenApiValue.fromRaw(it) }
            }.toMap(),
            parameters = OpenApiJson.optionalArray(operation["parameters"], "parameters").orEmpty().map { rawParameter ->
                val parameter = OpenApiJson.objectValue(rawParameter, "parameter")
                val schema = OpenApiJson.optionalObject(parameter["schema"], "parameter schema")
                OpenApiParameterModel(
                    location = OpenApiJson.string(parameter["in"], "parameter location"),
                    name = OpenApiJson.string(parameter["name"], "parameter name"),
                    required = OpenApiJson.boolean(parameter["required"], "parameter required"),
                    type = schema?.let { OpenApiJson.string(it["type"], "parameter schema type") }
                )
            }.toSet(),
            responses = responses,
            responseSchemas = responseSchemas,
            requestSchema = schemaReference(operation["requestBody"], "request content")
                ?: operation["x-f95gm-request-schema"] as? String,
            responseSchema = operation["x-f95gm-response-schema"] as? String
                ?: schemaReference(successResponse, "response content")
        )
    }

    private fun parseSchemas(document: Map<*, *>): Map<String, OpenApiSchemaModel> {
        val components = OpenApiJson.optionalObject(document["components"], "components") ?: return emptyMap()
        val schemas = OpenApiJson.optionalObject(components["schemas"], "components.schemas") ?: return emptyMap()
        return schemas.entries.associate { (rawName, rawSchema) ->
            val name = rawName.toString()
            val schema = OpenApiJson.objectValue(rawSchema, "schema $name")
            val properties = OpenApiJson.optionalObject(schema["properties"], "schema $name properties")?.entries?.associate { (rawProperty, rawValue) ->
                val property = OpenApiJson.objectValue(rawValue, "property $name.$rawProperty")
                rawProperty.toString() to OpenApiPropertyModel(
                    OpenApiJson.string(property["type"], "property $name.$rawProperty type"),
                    OpenApiJson.string(property["format"], "property $name.$rawProperty format"),
                    OpenApiJson.string(property["\$ref"], "property $name.$rawProperty reference"),
                    property.constraints(), property["enum"].asOpenApiValues("property $name.$rawProperty enum"),
                    schemaNode(property, "property $name.$rawProperty")
                )
            } ?: emptyMap()
            name to OpenApiSchemaModel(
                type = OpenApiJson.string(schema["type"], "schema $name type"),
                required = OpenApiJson.optionalStrings(schema["required"], "schema $name required")?.toSet().orEmpty(),
                properties = properties,
                constraints = schema.constraints(),
                enumValues = schema["enum"].asOpenApiValues("schema $name enum"),
                node = schemaNode(schema, "schema $name")
            )
        }
    }

    private fun schemaReference(value: Any?, context: String): String? {
        val container = OpenApiJson.optionalObject(value, context) ?: return null
        val content = container["content"] ?: return null
        val mediaTypes = OpenApiJson.objectValue(content, context)
        return mediaTypes.values.firstNotNullOfOrNull { rawMedia ->
            val media = OpenApiJson.optionalObject(rawMedia, "$context media") ?: return@firstNotNullOfOrNull null
            val schema = OpenApiJson.optionalObject(media["schema"], "$context schema") ?: return@firstNotNullOfOrNull null
            OpenApiJson.string(schema["\$ref"], "$context schema reference")?.substringAfterLast('/')
        }
    }

    private fun Map<*, *>.constraints(): Map<String, OpenApiValue> =
        schemaConstraints.mapNotNull { key ->
            this[key]?.let { key to OpenApiValue.fromRaw(it) }
        }.toMap()

    private fun Any?.asOpenApiValues(context: String): List<OpenApiValue>? =
        OpenApiJson.optionalArray(this, context)?.map(OpenApiValue::fromRaw)

    private fun schemaNode(value: Map<*, *>, context: String, depth: Int = 0): OpenApiSchemaNode {
        check(depth < 64) { "OpenAPI schema nesting is too deep at $context" }
        val properties = OpenApiJson.optionalObject(value["properties"], "$context.properties")?.entries?.associate { (name, rawProperty) ->
            name.toString() to schemaNode(OpenApiJson.objectValue(rawProperty, "$context.$name"), "$context.$name", depth + 1)
        } ?: emptyMap()
        val alternatives = listOf("oneOf", "allOf", "anyOf").associateWith { key ->
            OpenApiJson.optionalArray(value[key], "$context.$key")?.mapIndexed { index, raw ->
                schemaNode(OpenApiJson.objectValue(raw, "$context.$key[$index]"), "$context.$key[$index]", depth + 1)
            }.orEmpty()
        }.filterValues { it.isNotEmpty() }
        val rawAdditional = value["additionalProperties"]
        return OpenApiSchemaNode(
            type = OpenApiJson.string(value["type"], "$context.type"),
            format = OpenApiJson.string(value["format"], "$context.format"),
            reference = OpenApiJson.string(value["\$ref"], "$context.\$ref"),
            constraints = value.constraints(),
            enumValues = value["enum"].asOpenApiValues("$context.enum"),
            required = OpenApiJson.optionalStrings(value["required"], "$context.required")?.toSet().orEmpty(),
            properties = properties,
            items = OpenApiJson.optionalObject(value["items"], "$context.items")?.let { schemaNode(it, "$context.items", depth + 1) },
            additionalProperties = rawAdditional.takeUnless { it is Boolean }?.let {
                OpenApiJson.optionalObject(it, "$context.additionalProperties")
            }?.let {
                schemaNode(it, "$context.additionalProperties", depth + 1)
            },
            additionalPropertiesAllowed = rawAdditional as? Boolean,
            alternatives = alternatives
        )
    }
}


