package f95gm.domain.api

import kotlinx.serialization.json.JsonObject

internal fun detailSchemaShape(schema: ApiSchema): JsonObject? = when (schema) {
    ApiSchema.THREAD_DETAIL -> propertiesSchema(
        "threadId" to "string", "title" to "string", "creator" to "string", "date" to "string",
        "cover" to "string", "meta" to "array:DetailMeta", "overviewHtml" to "string",
        "sections" to "array:DetailSection", "bodyHtml" to "string"
    )
    ApiSchema.DETAIL_META -> propertiesSchema("label" to "string", "value" to "string")
    ApiSchema.DETAIL_SECTION -> propertiesSchema("title" to "string", "html" to "string")
    else -> null
}
