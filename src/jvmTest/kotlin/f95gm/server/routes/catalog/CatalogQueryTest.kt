package f95gm.server.routes.catalog

import kotlin.test.Test
import kotlin.test.assertTrue

class CatalogQueryTest {
    @Test
    fun encodesRepeatedParametersAndReservedCharacters() {
        val query = CatalogQuery(
            category = "games & apps",
            page = CatalogPage(1),
            rows = 50,
            sort = "date",
            cacheBuster = 42,
            tags = listOf("12"),
            excludedTags = emptyList(),
            prefixes = emptyList(),
            excludedPrefixes = emptyList(),
            tagType = "and",
            date = 0,
            search = "a+b",
            creator = ""
        ).encoded()

        assertTrue(query.contains("cat=games+%26+apps"))
        assertTrue(query.contains("tags%5B%5D=12"))
        assertTrue(query.contains("tagtype=and"))
        assertTrue(query.contains("search=a%2Bb"))
        assertTrue(!query.contains("creator="))
    }
}
