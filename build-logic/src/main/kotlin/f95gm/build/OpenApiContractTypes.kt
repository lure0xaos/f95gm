package f95gm.build

/** Typed representation of the OpenAPI surface used by generators and compatibility checks. */
data class OpenApiDocumentModel(
    val openApi: String?,
    val operations: List<OpenApiOperationModel>,
    val schemas: Map<String, OpenApiSchemaModel>
)

sealed interface OpenApiScalar {
    data class Text(val value: String) : OpenApiScalar
    data class NumberValue(val value: Number) : OpenApiScalar
    data class BooleanValue(val value: Boolean) : OpenApiScalar
}

/** Typed wrapper for scalar JSON values crossing the Groovy parser boundary. */
data class OpenApiValue(val scalar: OpenApiScalar?) {
    val booleanOrNull: Boolean? get() = (scalar as? OpenApiScalar.BooleanValue)?.value

    companion object {
        fun fromRaw(raw: Any?): OpenApiValue = OpenApiValue(
            when (raw) {
                null -> null
                is String -> OpenApiScalar.Text(raw)
                is Number -> OpenApiScalar.NumberValue(raw)
                is Boolean -> OpenApiScalar.BooleanValue(raw)
                else -> error("OpenAPI schema values must be JSON scalars: ${raw::class.qualifiedName}")
            }
        )
    }
}

data class OpenApiOperationModel(
    val path: String,
    val method: String,
    val operationId: String?,
    val metadata: Map<String, OpenApiValue>,
    val parameters: Set<OpenApiParameterModel>,
    val responses: Map<String, Set<String>>,
    val responseSchemas: Set<String>,
    val requestSchema: String?,
    val responseSchema: String?
)

data class OpenApiParameterModel(val location: String?, val name: String?, val required: Boolean?, val type: String?)

data class OpenApiSchemaModel(
    val type: String?,
    val required: Set<String>,
    val properties: Map<String, OpenApiPropertyModel>,
    val constraints: Map<String, OpenApiValue>,
    val enumValues: List<OpenApiValue>?,
    val node: OpenApiSchemaNode
)

data class OpenApiPropertyModel(
    val type: String?,
    val format: String?,
    val reference: String?,
    val constraints: Map<String, OpenApiValue>,
    val enumValues: List<OpenApiValue>?,
    val node: OpenApiSchemaNode
)

data class OpenApiSchemaNode(
    val type: String?,
    val format: String?,
    val reference: String?,
    val constraints: Map<String, OpenApiValue>,
    val enumValues: List<OpenApiValue>?,
    val required: Set<String>,
    val properties: Map<String, OpenApiSchemaNode>,
    val items: OpenApiSchemaNode?,
    val additionalProperties: OpenApiSchemaNode?,
    val additionalPropertiesAllowed: Boolean?,
    val alternatives: Map<String, List<OpenApiSchemaNode>>
)


