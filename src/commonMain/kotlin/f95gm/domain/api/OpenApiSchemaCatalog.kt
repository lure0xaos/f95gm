package f95gm.domain.api

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject

/** One registry for schema ownership, publication, required fields, and generated shapes. */
internal object OpenApiSchemaCatalog {
    private val generatorOwned = setOf(ApiSchema.JSON_OBJECT, ApiSchema.API_ERROR, ApiSchema.UPSTREAM_CATALOG, ApiSchema.MEDIA)

    /** Stable output order is part of the checked-in OpenAPI artifact contract. */
    val published: List<ApiSchema> = listOf(
        ApiSchema.LOGIN_REQUEST, ApiSchema.OK_RESPONSE, ApiSchema.CATALOG_OPTIONS,
        ApiSchema.MARKS, ApiSchema.SAVED_FILTERS, ApiSchema.SAVED_FILTER_REQUEST,
        ApiSchema.SAVED_FILTER, ApiSchema.SAVED_FILTER_OPTION, ApiSchema.QUICK_LINKS_BACKUP,
        ApiSchema.IMPORTED_QUICK_LINKS, ApiSchema.MARK_REQUEST, ApiSchema.MARK_RESPONSE,
        ApiSchema.THREAD_DETAIL, ApiSchema.DETAIL_META, ApiSchema.DETAIL_SECTION,
        ApiSchema.REQUEST_METRICS, ApiSchema.EMPTY_REQUEST
    ).also { ordered ->
        check(ordered.toSet() + generatorOwned == ApiSchema.entries.toSet()) {
            "Every API schema must be classified as published or generator-owned"
        }
    }

    fun shape(schema: ApiSchema): JsonObject = when (schema) {
        ApiSchema.JSON_OBJECT -> objectSchema(additionalProperties = true)
        ApiSchema.API_ERROR -> apiErrorShape()
        ApiSchema.LOGIN_REQUEST, ApiSchema.OK_RESPONSE, ApiSchema.MARK_REQUEST -> schema.openApiShape()
        else -> moduleSchemaShape(schema)
            ?: throw ApiContractInvariantViolation("No OpenAPI schema module owns ${schema.wireName}")
    }

    fun requiredFields(schema: ApiSchema): List<String> = when (schema) {
        ApiSchema.LOGIN_REQUEST -> listOf("login", "password")
        ApiSchema.OK_RESPONSE -> listOf("ok")
        ApiSchema.MARK_REQUEST -> listOf("trackingState")
        else -> emptyList()
    }

    fun typedShape(schema: ApiSchema): JsonObject = when (schema) {
        ApiSchema.LOGIN_REQUEST -> buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("required", buildJsonArray { add(JsonPrimitive("login")); add(JsonPrimitive("password")) })
            put("properties", buildJsonObject {
                put("login", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("password", buildJsonObject { put("type", JsonPrimitive("string")) })
            })
        }
        ApiSchema.OK_RESPONSE -> buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("required", buildJsonArray { add(JsonPrimitive("ok")) })
            put("properties", buildJsonObject { put("ok", buildJsonObject { put("type", JsonPrimitive("boolean")) }) })
        }
        ApiSchema.MARK_REQUEST -> buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("trackingState", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("storedVersion", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("acknowledge", buildJsonObject { put("type", JsonPrimitive("boolean")) })
                put("expectedUpdatedAt", buildJsonObject { put("type", JsonPrimitive("integer")) })
            })
        }
        else -> buildJsonObject { put("type", JsonPrimitive("object")) }
    }

    private fun apiErrorShape(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("required", buildJsonArray {
            add(JsonPrimitive("message"))
            add(JsonPrimitive("code"))
        })
        put("properties", buildJsonObject {
            put("message", buildJsonObject { put("type", JsonPrimitive("string")) })
            put("code", buildJsonObject { put("type", JsonPrimitive("string")) })
            put("messageKey", buildJsonObject { put("type", JsonPrimitive("string")) })
            put("details", buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("additionalProperties", buildJsonObject { put("type", JsonPrimitive("string")) })
            })
        })
    }
}
