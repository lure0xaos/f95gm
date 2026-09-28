package f95gm.domain.api

/** Explicit schema ownership prevents the OpenAPI generator from becoming a second registry. */
internal enum class OpenApiSchemaModule {
    CATALOG,
    FILTERS,
    MARKS,
    DETAIL,
    OPERATIONAL
}

internal fun ApiSchema.module(): OpenApiSchemaModule = when (this) {
    ApiSchema.CATALOG_OPTIONS, ApiSchema.UPSTREAM_CATALOG -> OpenApiSchemaModule.CATALOG
    ApiSchema.SAVED_FILTERS, ApiSchema.SAVED_FILTER_REQUEST,
    ApiSchema.SAVED_FILTER, ApiSchema.SAVED_FILTER_OPTION,
    ApiSchema.QUICK_LINKS_BACKUP, ApiSchema.IMPORTED_QUICK_LINKS -> OpenApiSchemaModule.FILTERS
    ApiSchema.MARKS, ApiSchema.MARK_REQUEST, ApiSchema.MARK_RESPONSE -> OpenApiSchemaModule.MARKS
    ApiSchema.THREAD_DETAIL, ApiSchema.DETAIL_META, ApiSchema.DETAIL_SECTION,
    ApiSchema.MEDIA -> OpenApiSchemaModule.DETAIL
    ApiSchema.JSON_OBJECT, ApiSchema.API_ERROR, ApiSchema.LOGIN_REQUEST,
    ApiSchema.OK_RESPONSE, ApiSchema.REQUEST_METRICS, ApiSchema.EMPTY_REQUEST ->
        OpenApiSchemaModule.OPERATIONAL
}

internal fun schemaModuleInventory(): Map<OpenApiSchemaModule, Set<ApiSchema>> =
    ApiSchema.entries.groupBy(ApiSchema::module).mapValues { (_, schemas) -> schemas.toSet() }

internal fun validateSchemaModuleInventory(inventory: Map<OpenApiSchemaModule, Set<ApiSchema>>) {
    val schemas = inventory.values.flatten()
    require(schemas.size == schemas.toSet().size) { "A schema cannot belong to multiple modules" }
    require(schemas.toSet() == ApiSchema.entries.toSet()) { "Schema module inventory is incomplete" }
}

internal fun moduleSchemaShape(schema: ApiSchema) =
    catalogSchemaShape(schema)
        ?: filterSchemaShape(schema)
        ?: markSchemaShape(schema)
        ?: detailSchemaShape(schema)
        ?: operationalSchemaShape(schema)
