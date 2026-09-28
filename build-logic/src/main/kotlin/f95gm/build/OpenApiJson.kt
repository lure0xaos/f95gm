package f95gm.build

import groovy.json.JsonSlurper

/** Small typed boundary around the untyped JSON tree used by build-time contract checks. */
internal typealias OpenApiObject = Map<String, Any?>
internal typealias OpenApiArray = List<Any?>

object OpenApiJson {
    fun parse(text: String, context: String = "OpenAPI document"): OpenApiObject =
        JsonSlurper().parseText(text).asObject(context)

    private fun Any?.asObject(context: String): OpenApiObject =
        (this as? Map<*, *>)?.entries?.associate { (key, value) ->
            require(key is String) { "OpenAPI $context object keys must be strings" }
            key to value
        } ?: error("$context must be a JSON object")

    fun objectValue(value: Any?, context: String): OpenApiObject = value.asObject("OpenAPI $context")

    fun optionalObject(value: Any?, context: String): OpenApiObject? =
        if (value == null) null else value.asObject("OpenAPI $context")

    fun memberObject(parent: Map<*, *>, name: String, context: String): OpenApiObject =
        objectValue(parent[name], "$context.$name")

    fun string(value: Any?, context: String): String? = when (value) {
        null -> null
        is String -> value
        else -> error("OpenAPI $context must be a string")
    }

    fun boolean(value: Any?, context: String): Boolean? = when (value) {
        null -> null
        is Boolean -> value
        else -> error("OpenAPI $context must be a boolean")
    }

    fun array(value: Any?, context: String): OpenApiArray = when (value) {
        null -> emptyList()
        is List<*> -> value.toList()
        else -> error("OpenAPI $context must be an array")
    }

    fun optionalArray(value: Any?, context: String): OpenApiArray? =
        if (value == null) null else array(value, context)

    fun strings(value: Any?, context: String): List<String> =
        array(value, context).mapIndexed { index, item ->
            string(item, "$context[$index]") ?: error("OpenAPI $context[$index] must not be null")
        }

    fun optionalStrings(value: Any?, context: String): List<String>? =
        if (value == null) null else strings(value, context)
}
