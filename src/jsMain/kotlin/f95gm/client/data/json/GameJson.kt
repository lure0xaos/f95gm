package f95gm.client.data.json

import kotlinx.serialization.json.*

internal fun JsonObject.gameDeveloper(): String = string("creator").ifBlank { string("developer") }

internal fun JsonObject.gameCover(): String = string("cover").ifBlank {
    this["images"]?.jsonObject?.string("cover").orEmpty()
}

internal fun JsonObject.gamePrefixes(): List<String> = this["prefixes"]?.jsonArray
    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
    .orEmpty()
