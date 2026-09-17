package f95gm.client.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClientPayloadsTest {
    @Test
    fun catalogPayloadsDecodeWireNamesAndDefaults() {
        val json = Json { ignoreUnknownKeys = true }
        val catalog = json.decodeFromString<CatalogPayload>("""{"status":"ok","msg":{"data":[]}}""")
        val item = json.decodeFromString<CatalogItemPayload>(
            """{"thread_id":"123","title":"Game","version":"1.2","creator":"author","developer":"studio","cover":"/cover.jpg","images":{"cover":"/cover.jpg"},"tags":["rpg"],"prefixes":["completed"],"date":"today","likes":"4","views":"90","rating":4.5,"new":true}"""
        )
        val detail = json.decodeFromString<DetailPayload>(
            """{"threadId":"123","title":"Game","creator":"author","date":"today","cover":"/cover.jpg","meta":[{"label":"Version","value":"1.2"}],"overviewHtml":"overview","sections":[{"title":"Info","html":"body"}],"bodyHtml":"full"}"""
        )
        val meta = json.decodeFromString<DetailMetaPayload>("""{"label":"Version","value":"1.2"}""")
        val section = json.decodeFromString<DetailSectionPayload>("""{"title":"Info","html":"body"}""")

        assertEquals("ok", catalog.status)
        assertEquals("123", item.threadId)
        assertEquals("Game", item.title)
        assertEquals("1.2", item.version)
        assertEquals("author", item.creator)
        assertEquals("studio", item.developer)
        assertEquals("/cover.jpg", item.cover)
        assertEquals("/cover.jpg", item.images.jsonObject.getValue("cover").jsonPrimitive.content)
        assertEquals("rpg", item.tags.jsonArray.single().jsonPrimitive.content)
        assertEquals("completed", item.prefixes.jsonArray.single().jsonPrimitive.content)
        assertEquals("today", item.date)
        assertEquals("4", item.likes)
        assertEquals("90", item.views)
        assertEquals("4.5", item.rating.jsonPrimitive.content)
        assertEquals("true", item.isNew.jsonPrimitive.content)
        assertEquals("123", detail.threadId)
        assertEquals("Game", detail.title)
        assertEquals("author", detail.creator)
        assertEquals("today", detail.date)
        assertEquals("/cover.jpg", detail.cover)
        assertNotNull(detail.meta)
        assertEquals("overview", detail.overviewHtml)
        assertNotNull(detail.sections)
        assertEquals("full", detail.bodyHtml)
        assertEquals("Version", meta.label)
        assertEquals("1.2", meta.value)
        assertEquals("Info", section.title)
        assertEquals("body", section.html)
        assertEquals("", json.decodeFromString<CatalogImagesPayload>("{}").cover)
    }

    @Test
    fun markAndSavedFilterPayloadsRetainOptionalAndCollectionFields() {
        val json = Json { ignoreUnknownKeys = true }
        val mark = json.decodeFromString<MarkPayload>(
            """{"threadId":"123","trackingState":"DOWNLOADED","title":"Game","version":"1.2","creator":null,"developer":"studio","cover":"cover","prefixes":["rpg"],"date":"today","likes":"3","views":"40","rating":4.0,"isNew":true,"storedVersion":"1.0","latestVersion":"1.2","updateAvailable":true,"updatedAt":10}"""
        )
        val filter = json.decodeFromString<SavedFilterPayload>(
            """{"id":"favorites","name":"Favorites","category":"games","sort":"date","search":"rpg","creatorSearch":"studio","searchMode":"creator","selectedTags":[{"id":"tag","name":"Tag"}],"excludedTags":[],"selectedPrefixes":[{"id":"done","name":"Done"}],"excludedPrefixes":[],"tagType":"and","dateLimit":30,"updatedAt":20}"""
        )
        val option = json.decodeFromString<FilterOptionPayload>("""{"id":"tag","name":"Tag"}""")

        assertEquals("123", mark.threadId)
        assertEquals("DOWNLOADED", mark.trackingState)
        assertEquals("Game", mark.title)
        assertEquals("1.2", mark.version)
        assertEquals(null, mark.creator)
        assertEquals("studio", mark.developer)
        assertEquals("cover", mark.cover)
        assertEquals(listOf("rpg"), mark.prefixes)
        assertEquals("today", mark.date)
        assertEquals("3", mark.likes)
        assertEquals("40", mark.views)
        assertEquals(4.0, mark.rating)
        assertEquals(true, mark.isNew)
        assertEquals("1.0", mark.storedVersion)
        assertEquals("1.2", mark.latestVersion)
        assertEquals(true, mark.updateAvailable)
        assertEquals(10L, mark.updatedAt)
        assertEquals("favorites", filter.id)
        assertEquals("Favorites", filter.name)
        assertEquals("games", filter.category)
        assertEquals("date", filter.sort)
        assertEquals("rpg", filter.search)
        assertEquals("studio", filter.creatorSearch)
        assertEquals("creator", filter.searchMode)
        assertEquals("tag", filter.selectedTags.single().id)
        assertEquals("Tag", filter.selectedTags.single().name)
        assertEquals(0, filter.excludedTags.size)
        assertEquals("done", filter.selectedPrefixes.single().id)
        assertEquals("Done", filter.selectedPrefixes.single().name)
        assertEquals(0, filter.excludedPrefixes.size)
        assertEquals("and", filter.tagType)
        assertEquals(30, filter.dateLimit)
        assertEquals(20L, filter.updatedAt)
        assertEquals("tag", option.id)
        assertEquals("Tag", option.name)
        assertEquals("WATCHING", MarkPayload().trackingState)
        assertEquals("games", SavedFilterPayload().category)
        assertEquals(1, CatalogPaginationPayload().total)
    }
}
