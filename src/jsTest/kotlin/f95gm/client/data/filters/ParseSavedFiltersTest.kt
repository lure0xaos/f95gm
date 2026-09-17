package f95gm.client.data.filters

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.filters.F95Category
import f95gm.domain.filters.F95SearchMode
import f95gm.domain.filters.F95Sort
import f95gm.domain.filters.F95TagType
import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseSavedFiltersTest {
    @Test
    fun mapsFilterFieldsAndOptionCollections() {
        val filters = parseSavedFilters(
            """
                [{
                  "id":"filter-1","name":"Favorites","category":"games","sort":"date",
                  "search":"demo","creatorSearch":"dev","searchMode":"creator","tagType":"and",
                  "selectedTags":[{"id":"1","name":"Tag"}],
                  "excludedPrefixes":[{"id":"2"}],
                  "dateLimit":7,"updatedAt":42
                }]
            """.trimIndent()
        )

        val filter = filters.single()
        assertEquals("filter-1", filter.id.value)
        assertEquals("Favorites", filter.name.value)
        assertEquals(F95Category.GAMES, filter.category)
        assertEquals(F95SearchMode.CREATOR, filter.searchMode)
        assertEquals(F95TagType.AND, filter.tagType)
        assertEquals("Tag", filter.selectedTags.single().name.value)
        assertEquals("2", filter.excludedPrefixes.single().name.value)
        assertEquals(7, filter.dateLimit)
        assertEquals(42L, filter.updatedAt.value)
    }

    @Test
    fun suppliesDefaultsAndSkipsBlankOptionIds() {
        val filter = parseSavedFilters(
            """
                [{
                  "id":"filter-2",
                  "selectedTags":[{"id":"","name":"Ignored"},{"id":"3"}]
                }]
            """.trimIndent()
        ).single()

        assertEquals(UiMessages.app_unnamedFilter(), filter.name.value)
        assertEquals(F95Defaults.ANY_DATE, filter.dateLimit)
        assertEquals(0L, filter.updatedAt.value)
        assertEquals("3", filter.selectedTags.single().name.value)
    }

    @Test
    fun fallsBackForUnknownEnumWireValues() {
        val filter = parseSavedFilters(
            """
                [{"id":"filter-3","category":"future","sort":"future","searchMode":"future","tagType":"future"}]
            """.trimIndent()
        ).single()

        assertEquals(F95Category.GAMES, filter.category)
        assertEquals(F95Sort.DATE, filter.sort)
        assertEquals(F95SearchMode.TITLE, filter.searchMode)
        assertEquals(F95TagType.OR, filter.tagType)
    }
}
