package f95gm.server.application

import f95gm.domain.api.FilterOptionRequest
import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.domain.defaults.F95Defaults
import f95gm.server.model.ParseResult
import f95gm.server.state.runtime.MutableServerClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SavedFilterRequestMapperTest {
    @Test
    fun trimsBoundsAndSanitizesFilterFields() {
        val clock = MutableServerClock(initialMillis = 1234)
        val request = SavedFilterRequest(
            name = F95OptionName("  Favorites  "),
            search = "  game query  ",
            creatorSearch = "  creator  ",
            selectedTags = listOf(
                FilterOptionRequest(F95OptionId(" tag "), F95OptionName(" Tag name ")),
                FilterOptionRequest(F95OptionId("tag"), F95OptionName("duplicate")),
                FilterOptionRequest(F95OptionId("  "), F95OptionName("ignored")),
                FilterOptionRequest(F95OptionId("fallback"), F95OptionName("  "))
            ),
            dateLimit = Int.MAX_VALUE
        )

        val filter = assertIs<ParseResult.Success<f95gm.server.model.filters.SavedFilter>>(
            request.toSavedFilterResult(clock)
        ).value
        assertEquals("Favorites", filter.name.value)
        assertEquals("favorites", filter.id.value)
        assertEquals("game query", filter.search)
        assertEquals("creator", filter.creatorSearch)
        assertEquals(F95Defaults.MAX_DATE_DAYS, filter.dateLimit)
        assertEquals(1234L, filter.updatedAt.value)
        assertEquals(listOf("tag", "fallback"), filter.selectedTags.map { it.id.value })
        assertEquals(listOf("Tag name", "fallback"), filter.selectedTags.map { it.name.value })
    }

    @Test
    fun blankNameIsRejected() {
        val request = SavedFilterRequest(name = F95OptionName("  "))
        assertIs<ParseResult.Invalid>(request.toSavedFilterResult(MutableServerClock()))
        assertNull(request.toSavedFilter(MutableServerClock()))
    }
}
