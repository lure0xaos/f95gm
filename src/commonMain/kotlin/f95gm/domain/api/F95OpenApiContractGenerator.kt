package f95gm.domain.api

import kotlinx.serialization.json.*


private val openApiJson = Json { prettyPrint = true; encodeDefaults = true }

/** Generates the machine-readable contract from the same endpoint descriptors used by routing. */
fun F95Api.openApiContractJson(): String {
    val paths = endpointDescriptors.groupBy { it.path }.mapValues { (_, descriptors) ->
        descriptors.flatMap { descriptor -> descriptor.methods.map { method -> descriptor to method } }
            .associate { (descriptor, method) ->
                method.wireValue.lowercase() to buildJsonObject {
                    put(
                        "operationId",
                        "${method.wireValue.lowercase()}_${
                            descriptor.path.trim('/').replace('/', '_').replace('{', '_').replace('}', '_')
                        }"
                    )
                    put("summary", operationSummary(descriptor.path, method))
                    put("description", operationDescription(descriptor.path, method))
                    put(
                        "tags",
                        kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(operationTag(descriptor.path))) })
                    put(
                        "security", if (descriptor.auth == F95Api.AuthRequirement.SESSION) {
                            kotlinx.serialization.json.buildJsonArray {
                                add(buildJsonObject {
                                    put(
                                        "session",
                                        buildJsonObject {})
                                })
                            }
                        } else {
                            kotlinx.serialization.json.buildJsonArray {}
                        })
                    put("x-f95gm-mutation", descriptor.mutation.name)
                    put("x-f95gm-rate-limit", descriptor.rateLimit.name)
                    if (descriptor.queryParameters.isNotEmpty() || descriptor.pathParameters.isNotEmpty()) {
                        put("parameters", kotlinx.serialization.json.buildJsonArray {
                            descriptor.pathParameters.forEach { name -> add(parameter(name, path = true)) }
                            descriptor.queryParameters.forEach { name -> add(parameter(name, path = false)) }
                        })
                    }
                    put("responses", buildJsonObject {
                        put(
                            descriptor.successStatus.code.toString(),
                            response("Successful response", descriptor.responseBody, descriptor.responseSchema)
                        )
                        descriptor.errorStatuses.sortedBy(F95ApiStatus::code).forEach { status ->
                            val body =
                                if (status in descriptor.emptyResponseStatuses) F95Api.ResponseBody.EMPTY else F95Api.ResponseBody.JSON
                            put(status.code.toString(), response(errorDescription(status.code), body, "ApiError"))
                        }
                    })
                    descriptor.requestSchema?.takeUnless { it == "EmptyRequest" }?.let { schema ->
                        put("requestBody", buildJsonObject {
                            put("required", true)
                            put("content", buildJsonObject {
                                put("application/json", buildJsonObject {
                                    put("schema", buildJsonObject { put("\$ref", "#/components/schemas/$schema") })
                                })
                            })
                        })
                    }
                }
            }
    }
    return openApiJson.encodeToString(
        F95OpenApiContract(paths = paths, components = apiComponents())
    )
}

private fun apiComponents(): JsonObject = buildJsonObject {
    put("schemas", buildJsonObject {
        put("ApiError", buildJsonObject {
            put("type", JsonPrimitive("object"))
            put(
                "required",
                kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("message")); add(JsonPrimitive("code")) })
            put("properties", buildJsonObject {
                put("message", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("code", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("messageKey", buildJsonObject { put("type", JsonPrimitive("string")) })
                put("details", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("additionalProperties", buildJsonObject { put("type", JsonPrimitive("string")) })
                })
            })
        })
        put("JsonObject", objectSchema(additionalProperties = true))
        listOf(
            "LoginRequest", "OkResponse", "CatalogOptions", "Marks", "SavedFilters", "SavedFilterRequest",
            "SavedFilter", "SavedFilterOption", "QuickLinksBackup", "ImportedQuickLinks", "MarkRequest",
            "MarkResponse", "ThreadDetail", "DetailMeta", "DetailSection", "RequestMetrics", "EmptyRequest"
        ).forEach { name -> put(name, namedSchema(name)) }
    })
}

private fun objectSchema(additionalProperties: Boolean = false): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("object"))
    if (additionalProperties) put("additionalProperties", JsonPrimitive(true))
}

private fun arraySchema(itemRef: String): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("array"))
    put(
        "items", if (itemRef.startsWith("primitive:")) {
            buildJsonObject { put("type", JsonPrimitive(itemRef.removePrefix("primitive:"))) }
        } else buildJsonObject { put("\$ref", "#/components/schemas/$itemRef") })
}

private fun propertiesSchema(vararg properties: Pair<String, String>): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("object"))
    put("properties", buildJsonObject {
        properties.forEach { (name, type) ->
            put(
                name, when {
                    type.startsWith("#") -> buildJsonObject { put("\$ref", type) }
                    type.startsWith("array:") -> arraySchema(type.removePrefix("array:"))
                    type == "string-map" -> buildJsonObject {
                        put("type", JsonPrimitive("object"))
                        put("additionalProperties", buildJsonObject { put("type", JsonPrimitive("string")) })
                    }

                    type == "number-map" -> buildJsonObject {
                        put("type", JsonPrimitive("object"))
                        put("additionalProperties", buildJsonObject { put("type", JsonPrimitive("integer")) })
                    }

                    type == "any-object-or-array" -> buildJsonObject {
                        put("anyOf", kotlinx.serialization.json.buildJsonArray {
                            add(buildJsonObject { put("type", JsonPrimitive("object")) })
                            add(buildJsonObject { put("type", JsonPrimitive("array")) })
                        })
                    }

                    else -> buildJsonObject { put("type", JsonPrimitive(type)) }
                })
        }
    })
}

private fun JsonObject.withRequired(vararg names: String): JsonObject = buildJsonObject {
    this@withRequired.forEach { (key, value) -> put(key, value) }
    put("required", kotlinx.serialization.json.buildJsonArray { names.forEach { add(JsonPrimitive(it)) } })
}

@Suppress("CyclomaticComplexMethod") // The schema registry is intentionally an exhaustive name-to-shape mapping.
private fun namedSchema(name: String): JsonObject = when (name) {
    "LoginRequest" -> buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "required",
            kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("login")); add(JsonPrimitive("password")) })
        put("properties", buildJsonObject {
            put("login", buildJsonObject { put("type", JsonPrimitive("string")) })
            put("password", buildJsonObject { put("type", JsonPrimitive("string")) })
        })
    }

    "OkResponse" -> buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("required", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("ok")) })
        put("properties", buildJsonObject { put("ok", buildJsonObject { put("type", JsonPrimitive("boolean")) }) })
    }

    "CatalogOptions" -> propertiesSchema("tags" to "string-map", "prefixes" to "any-object-or-array")
    "Marks" -> arraySchema("MarkResponse")
    "SavedFilters" -> arraySchema("SavedFilter")
    "SavedFilterOption" -> propertiesSchema("id" to "string", "name" to "string")
    "SavedFilterRequest" -> propertiesSchema(
        "id" to "string", "name" to "string", "category" to "string", "sort" to "string",
        "search" to "string", "creatorSearch" to "string", "searchMode" to "string",
        "selectedTags" to "array:SavedFilterOption", "excludedTags" to "array:SavedFilterOption",
        "selectedPrefixes" to "array:SavedFilterOption", "excludedPrefixes" to "array:SavedFilterOption",
        "tagType" to "string", "dateLimit" to "integer", "expectedUpdatedAt" to "integer"
    )

    "SavedFilter" -> propertiesSchema(
        "id" to "string", "name" to "string", "category" to "string", "sort" to "string",
        "search" to "string", "creatorSearch" to "string", "searchMode" to "string",
        "selectedTags" to "array:SavedFilterOption", "excludedTags" to "array:SavedFilterOption",
        "selectedPrefixes" to "array:SavedFilterOption", "excludedPrefixes" to "array:SavedFilterOption",
        "tagType" to "string", "dateLimit" to "integer", "updatedAt" to "integer"
    )

    "QuickLinksBackup" -> propertiesSchema("version" to "integer", "quickLinks" to "array:SavedFilterRequest")
    "ImportedQuickLinks" -> propertiesSchema("imported" to "integer")
    "MarkRequest" -> propertiesSchema(
        "trackingState" to "string",
        "storedVersion" to "string",
        "acknowledge" to "boolean",
        "expectedUpdatedAt" to "integer"
    ).withRequired("trackingState")

    "MarkResponse" -> propertiesSchema(
        "threadId" to "string",
        "trackingState" to "string",
        "title" to "string",
        "version" to "string",
        "developer" to "string",
        "cover" to "string",
        "prefixes" to "array:primitive:string",
        "date" to "string",
        "likes" to "string",
        "views" to "string",
        "rating" to "number",
        "isNew" to "boolean",
        "storedVersion" to "string",
        "latestVersion" to "string",
        "updateAvailable" to "boolean",
        "updatedAt" to "integer"
    )

    "ThreadDetail" -> propertiesSchema(
        "threadId" to "string",
        "title" to "string",
        "creator" to "string",
        "date" to "string",
        "cover" to "string",
        "meta" to "array:DetailMeta",
        "overviewHtml" to "string",
        "sections" to "array:DetailSection",
        "bodyHtml" to "string"
    )

    "DetailMeta" -> propertiesSchema("label" to "string", "value" to "string")
    "DetailSection" -> propertiesSchema("title" to "string", "html" to "string")
    "RequestMetrics" -> propertiesSchema(
        "requests" to "integer", "rejected" to "integer", "totalDurationMillis" to "integer", "retries" to "integer",
        "evictions" to "integer", "circuitOpenings" to "integer", "endpointRequests" to "number-map"
    )

    "EmptyRequest" -> objectSchema()
    else -> buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("description", JsonPrimitive("Route-specific $name payload."))
        put("additionalProperties", JsonPrimitive(true))
    }
}

@Suppress("CyclomaticComplexMethod") // Media type selection is centralized so every operation has one response contract.
private fun response(description: String, body: F95Api.ResponseBody, schema: String): JsonObject = buildJsonObject {
    put("description", description)
    if (body != F95Api.ResponseBody.EMPTY) {
        val mediaTypes = when {
            schema == "UpstreamCatalog" -> listOf("application/json", "text/html", "text/plain")
            schema == "Media" -> listOf("image/*")
            else -> listOf(
                when (body) {
                    F95Api.ResponseBody.JSON -> "application/json"
                    F95Api.ResponseBody.TEXT -> "text/plain"
                    F95Api.ResponseBody.BINARY -> "application/octet-stream"
                    F95Api.ResponseBody.EMPTY -> return@buildJsonObject
                }
            )
        }
        put("content", buildJsonObject {
            mediaTypes.forEach { mediaType ->
                put(mediaType, buildJsonObject {
                    put(
                        "schema", when {
                            schema == "UpstreamCatalog" && mediaType == "application/json" -> buildJsonObject {
                                put(
                                    "type",
                                    JsonPrimitive("object")
                                )
                            }

                            schema == "UpstreamCatalog" -> buildJsonObject { put("type", JsonPrimitive("string")) }
                            schema == "Media" -> buildJsonObject {
                                put("type", JsonPrimitive("string"))
                                put("format", JsonPrimitive("binary"))
                            }

                            else -> buildJsonObject { put("\$ref", "#/components/schemas/$schema") }
                        })
                })
            }
        })
    }
}

private fun parameter(name: String, path: Boolean): JsonObject = buildJsonObject {
    put("name", name)
    put("in", if (path) "path" else "query")
    put("required", path)
    val array = !path && name in setOf(F95Api.TAGS, F95Api.EXCLUDED_TAGS, F95Api.PREFIXES, F95Api.EXCLUDED_PREFIXES)
    val type = when {
        array -> "array"
        name in setOf(F95Api.PAGE, F95Api.ROWS, F95Api.DATE) -> "integer"
        else -> "string"
    }
    if (array) {
        put("style", "form")
        put("explode", true)
    }
    put("schema", buildJsonObject {
        put("type", JsonPrimitive(type))
        if (array) put("items", buildJsonObject { put("type", JsonPrimitive("string")) })
    })
}

private fun errorDescription(status: Int): String = when (status) {
    400 -> "Invalid request"
    401 -> "Authentication required"
    403 -> "Request forbidden"
    404 -> "Resource not found"
    409 -> "Request conflicts with current state"
    413 -> "Request body too large"
    415 -> "Unsupported request media type"
    429 -> "Rate limit exceeded"
    500 -> "Request failed"
    502 -> "Upstream request failed"
    503 -> "Service unavailable"
    504 -> "Upstream request timed out"
    else -> "Request failed"
}

private fun operationTag(path: String): String = when {
    path.startsWith("/api/login") || path.startsWith("/api/logout") || path.startsWith("/api/session") -> "Session"
    path.startsWith("/api/catalog") -> "Catalog"
    path.startsWith("/api/marks") -> "Marks"
    path.startsWith("/api/filters") -> "Saved filters"
    path.startsWith("/api/item") || path.startsWith("/api/media") -> "Items"
    else -> "Server"
}

private val operationSummaries = mapOf(
    (F95Api.LOGIN to null) to "Create a browser session",
    (F95Api.LOGOUT to null) to "End the current browser session",
    (F95Api.SESSION to null) to "Check the current browser session",
    (F95Api.CATALOG to null) to "Load catalog releases",
    (F95Api.CATALOG_OPTIONS to null) to "Load catalog filter options",
    (F95Api.MARKS to null) to "List marked releases",
    (F95Api.MARKS_CHECK to null) to "Check marked releases for updates",
    (F95Api.FILTERS to F95Api.ApiMethod.GET) to "List saved filters",
    (F95Api.FILTERS to F95Api.ApiMethod.POST) to "Save a filter",
    (F95Api.FILTERS_EXPORT to null) to "Export saved filters",
    (F95Api.FILTERS_IMPORT to null) to "Import saved filters",
    (F95Api.FILTER_BY_ID to null) to "Delete a saved filter",
    (F95Api.MARK_BY_THREAD to F95Api.ApiMethod.PUT) to "Save a release mark",
    (F95Api.MARK_BY_THREAD to null) to "Delete a release mark",
    (F95Api.ITEM_BY_THREAD to null) to "Load release details",
    (F95Api.MEDIA to null) to "Load proxied release media",
    (F95Api.HEALTH to null) to "Check server health",
    (F95Api.LIVE to null) to "Check server liveness",
    (F95Api.READY to null) to "Check server readiness",
    (F95Api.METRICS to null) to "Read server request metrics",
    (F95Api.SHUTDOWN to null) to "Request server shutdown"
)

private fun operationSummary(path: String, method: F95Api.ApiMethod): String =
    operationSummaries[path to method] ?: operationSummaries[path to null] ?: "Handle API request"

private fun operationDescription(path: String, method: F95Api.ApiMethod): String = when (path) {
    F95Api.CATALOG -> "Returns the upstream catalog payload as received by the server; the upstream may choose JSON or text."
    F95Api.MEDIA -> "Streams an allowed upstream media resource as binary content."
    F95Api.READY -> "Returns an empty 200 response when ready, or an empty 503 response while draining."
    F95Api.SHUTDOWN -> "Returns an empty 202 response when the shutdown request is accepted."
    F95Api.SESSION -> "Verifies the browser session against the upstream and returns an API error when no valid session exists."
    else -> "${method.wireValue} operation for ${operationTag(path).lowercase()}."
}
