package f95gm.domain.api

import kotlinx.serialization.json.*


private val openApiJson = Json { prettyPrint = true; encodeDefaults = true }

/** Generates the machine-readable contract from the same endpoint descriptors used by routing. */
fun openApiContractJson(): String {
    val paths = endpointDescriptors.groupBy { it.path.value }.mapValues { (_, descriptors) ->
        descriptors.flatMap { descriptor -> descriptor.methods.map { method -> descriptor to method } }
            .associate { (descriptor, method) ->
                method.wireValue.lowercase() to buildJsonObject {
                    put(
                        "operationId",
                        descriptor.operationId(method).value
                    )
                    put("summary", descriptor.documentation.summary)
                    put("description", descriptor.documentation.description)
                    put(
                        "tags",
                        kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(descriptor.tag.wireName)) })
                    put(
                        "security", if (descriptor.auth == AuthRequirement.SESSION) {
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
                    put("x-f95gm-api-version", descriptor.apiVersion.value)
                    put("x-f95gm-response-schema", descriptor.responseSchema.wireName)
                    descriptor.requestSchema?.let { put("x-f95gm-request-schema", it.wireName) }
                    descriptor.deprecation?.let { deprecation ->
                        put("deprecated", JsonPrimitive(true))
                        put("x-f95gm-deprecation-replacement", JsonPrimitive(deprecation.replacement.value))
                        put("x-f95gm-deprecation-sunset", JsonPrimitive(deprecation.sunset.value))
                    }
                    if (descriptor.queryParameters.isNotEmpty() || descriptor.pathParameters.isNotEmpty()) {
                        put("parameters", kotlinx.serialization.json.buildJsonArray {
                            descriptor.pathParameters.forEach { name -> add(parameter(name)) }
                            descriptor.queryParameters.forEach { name -> add(parameter(name)) }
                        })
                    }
                    put("responses", buildJsonObject {
                        put(
                            descriptor.successStatus.code.toString(),
                            response(descriptor.successStatus.description, descriptor.responseBody, descriptor.responseSchema)
                        )
                        descriptor.errorStatuses.sortedBy(F95ApiStatus::code).forEach { status ->
                            val body =
                                if (status in descriptor.emptyResponseStatuses) ResponseBody.EMPTY else ResponseBody.JSON
                            put(status.code.toString(), response(status.description, body, ApiSchema.API_ERROR))
                        }
                    })
                    descriptor.requestSchema?.takeUnless { it == ApiSchema.EMPTY_REQUEST }?.let { schema ->
                        put("requestBody", buildJsonObject {
                            put("required", true)
                            put("content", buildJsonObject {
                                put("application/json", buildJsonObject {
                                    put("schema", buildJsonObject { put("\$ref", "#/components/schemas/${schema.wireName}") })
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
