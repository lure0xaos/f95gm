package f95gm.client.data.filters

import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.catalog.PrefixOption
import f95gm.client.model.catalog.TagOption
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95PrefixId
import f95gm.domain.values.F95TagId
import kotlinx.serialization.json.*
import f95gm.client.test.required
import f95gm.client.test.requiredString
import f95gm.client.state.runtime.clientJson
import kotlin.test.Test
import kotlin.test.assertEquals
import f95gm.client.model.runtime.CatalogSearchQuery
import f95gm.client.model.runtime.DateLimit

class SavedFilterBodyTest {
    @Test
    fun serializesFilterStateUsingWireValuesAndOptionObjects() {
        val body = savedFilterBody(
            "Favorites",
            CatalogState(
                search = CatalogSearchQuery("demo"),
                creatorSearch = CatalogSearchQuery("author"),
                searchMode = F95SearchMode.CREATOR,
                selectedTags = listOf(TagOption(F95TagId("1"), F95OptionName("Tag"))),
                selectedPrefixes = listOf(PrefixOption(F95PrefixId("2"), F95OptionName("Prefix"))),
                dateLimit = DateLimit(7)
            )
        )
        val json = Json.parseToJsonElement(clientJson.encodeToString(body)).jsonObject

        assertEquals("Favorites", json.requiredString("name"))
        assertEquals("creator", json.requiredString("searchMode"))
        assertEquals("demo", json.requiredString("search"))
        assertEquals("1", json.required("selectedTags").jsonArray.single().jsonObject.requiredString("id"))
        assertEquals("Prefix", json.required("selectedPrefixes").jsonArray.single().jsonObject.requiredString("name"))
        assertEquals(7, json.required("dateLimit").jsonPrimitive.intOrNull)
    }
}
