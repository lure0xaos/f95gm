package f95gm.client.model.filters

import kotlinx.serialization.json.Json

/** Validated JSON document accepted by the quick-links import endpoint. */
internal class ImportedFiltersDocument private constructor(val value: String) {
    companion object {
        fun parse(raw: String): ImportedFiltersDocument {
            Json.parseToJsonElement(raw)
            return ImportedFiltersDocument(raw)
        }
    }
}
