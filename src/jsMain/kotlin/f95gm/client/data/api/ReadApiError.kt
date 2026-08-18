package f95gm.client.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun readApiError(body: String, fallback: String): String = runCatching {
    Json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.contentOrNull
        ?: Json.parseToJsonElement(body).jsonObject["msg"]?.jsonPrimitive?.contentOrNull
        ?: fallback
}.getOrDefault(fallback)
