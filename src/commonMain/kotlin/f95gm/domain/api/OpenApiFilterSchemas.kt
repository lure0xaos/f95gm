package f95gm.domain.api

import kotlinx.serialization.json.JsonObject

internal fun filterSchemaShape(schema: ApiSchema): JsonObject? = when (schema) {
    ApiSchema.SAVED_FILTERS -> arraySchema("SavedFilter")
    ApiSchema.SAVED_FILTER_OPTION -> propertiesSchema("id" to "string", "name" to "string")
    ApiSchema.SAVED_FILTER_REQUEST -> propertiesSchema(
        "id" to "string", "name" to "string", "category" to "string", "sort" to "string",
        "search" to "string", "creatorSearch" to "string", "searchMode" to "string",
        "selectedTags" to "array:SavedFilterOption", "excludedTags" to "array:SavedFilterOption",
        "selectedPrefixes" to "array:SavedFilterOption", "excludedPrefixes" to "array:SavedFilterOption",
        "tagType" to "string", "dateLimit" to "integer", "expectedUpdatedAt" to "integer"
    )
    ApiSchema.SAVED_FILTER -> propertiesSchema(
        "id" to "string", "name" to "string", "category" to "string", "sort" to "string",
        "search" to "string", "creatorSearch" to "string", "searchMode" to "string",
        "selectedTags" to "array:SavedFilterOption", "excludedTags" to "array:SavedFilterOption",
        "selectedPrefixes" to "array:SavedFilterOption", "excludedPrefixes" to "array:SavedFilterOption",
        "tagType" to "string", "dateLimit" to "integer", "updatedAt" to "integer"
    )
    ApiSchema.QUICK_LINKS_BACKUP -> propertiesSchema("version" to "integer", "quickLinks" to "array:SavedFilterRequest")
    ApiSchema.IMPORTED_QUICK_LINKS -> propertiesSchema("imported" to "integer")
    else -> null
}

