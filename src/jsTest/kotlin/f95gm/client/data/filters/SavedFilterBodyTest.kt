package f95gm.client.data.filters

import f95gm.client.model.catalog.CatalogState
import f95gm.client.model.catalog.PrefixOption
import f95gm.client.model.catalog.TagOption
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95PrefixId
import f95gm.domain.values.F95TagId
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedFilterBodyTest {
    @Test
    fun serializesFilterStateUsingWireValuesAndOptionObjects() {
        val body = savedFilterBody(
            "Favorites",
            CatalogState(
                search = "demo",
                creatorSearch = "author",
                searchMode = F95SearchMode.CREATOR,
                selectedTags = listOf(TagOption(F95TagId("1"), F95OptionName("Tag"))),
                selectedPrefixes = listOf(PrefixOption(F95PrefixId("2"), F95OptionName("Prefix"))),
                dateLimit = 7
            )
        )
        val json = Json.parseToJsonElement(body).jsonObject

        assertEquals("Favorites", json["name"]!!.jsonPrimitive.content)
        assertEquals("creator", json["searchMode"]!!.jsonPrimitive.content)
        assertEquals("demo", json["search"]!!.jsonPrimitive.content)
        assertEquals("1", json["selectedTags"]!!.jsonArray.single().jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("Prefix", json["selectedPrefixes"]!!.jsonArray.single().jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals(7, json["dateLimit"]!!.jsonPrimitive.intOrNull)
    }
}
