package f95gm.domain.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

internal fun apiComponents(): JsonObject = buildJsonObject {
    put("schemas", buildJsonObject {
        listOf(ApiSchema.API_ERROR, ApiSchema.JSON_OBJECT).plus(publishedOpenApiSchemas).forEach { schema ->
            put(schema.wireName, OpenApiSchemaCatalog.shape(schema).withRegistryRequirements(schema))
        }
    })
}

@Suppress("CyclomaticComplexMethod") // Media type selection is centralized for a stable response contract.
internal fun response(description: String, body: ResponseBody, schema: ApiSchema): JsonObject = buildJsonObject {
    put("description", JsonPrimitive(description))
    if (body != ResponseBody.EMPTY) {
        val mediaTypes = when {
            schema == ApiSchema.UPSTREAM_CATALOG -> listOf(ApiMediaType.JSON, ApiMediaType.HTML, ApiMediaType.TEXT)
            schema == ApiSchema.MEDIA -> listOf(ApiMediaType.IMAGE_WILDCARD)
            else -> listOf(
                when (body) {
                    ResponseBody.JSON -> ApiMediaType.JSON
                    ResponseBody.TEXT -> ApiMediaType.TEXT
                    ResponseBody.BINARY -> ApiMediaType.BINARY
                    ResponseBody.EMPTY -> return@buildJsonObject
                }
            )
        }
        put("content", buildJsonObject {
            mediaTypes.forEach { mediaType ->
                put(mediaType.wireName, buildJsonObject {
                    put("schema", when {
                        schema == ApiSchema.UPSTREAM_CATALOG && mediaType == ApiMediaType.JSON ->
                            buildJsonObject { put("type", JsonPrimitive("object")) }
                        schema == ApiSchema.UPSTREAM_CATALOG ->
                            buildJsonObject { put("type", JsonPrimitive("string")) }
                        schema == ApiSchema.MEDIA -> buildJsonObject {
                            put("type", JsonPrimitive("string")); put("format", JsonPrimitive("binary"))
                        }
                        else -> buildJsonObject {
                            put("$" + "ref", JsonPrimitive("#/components/schemas/${schema.wireName}"))
                        }
                    })
                })
            }
        })
    }
}

internal fun parameter(name: ApiQueryParameter): JsonObject = parameter(parameterDescriptor(name))

internal fun parameter(name: ApiPathParameter): JsonObject = parameter(parameterDescriptor(name))

private fun parameter(descriptor: ParameterDescriptor): JsonObject = buildJsonObject {
    val location = descriptor.location
    put("name", JsonPrimitive(descriptor.name))
    put("in", JsonPrimitive(location.name.lowercase()))
    put("required", JsonPrimitive(location == ParameterLocation.PATH))
    val array = descriptor.type == ParameterType.STRING_ARRAY
    val type = if (array) "array" else if (descriptor.type == ParameterType.INTEGER) "integer" else "string"
    if (array) {
        put("style", JsonPrimitive("form"))
        put("explode", JsonPrimitive(true))
    }
    put("schema", buildJsonObject {
        put("type", JsonPrimitive(type))
        if (array) put("items", buildJsonObject { put("type", JsonPrimitive("string")) })
    })
}
