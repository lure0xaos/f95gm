package f95gm.server.persistence.filters

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.server.model.filters.SavedFilter
import f95gm.domain.api.FilterOptionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertIs
import f95gm.server.model.ParseResult

class SavedFilterParsingTest {
    @Test
    fun rejectsBlankNamesAndNormalizesOptionsAndDateRange() {
        assertIs<ParseResult.Invalid>(parseSavedFilterResult(SavedFilter(name = F95OptionName(" "))))

        val parsed = (parseSavedFilterResult(
            SavedFilter(
                id = F95FilterId("filter"),
                name = F95OptionName(" Favorites "),
                dateLimit = Int.MAX_VALUE,
                selectedTags = listOf(
                    FilterOptionRequest(F95OptionId(" tag-1 "), F95OptionName("  ")),
                    FilterOptionRequest(F95OptionId("tag-1"), F95OptionName("duplicate")),
                    FilterOptionRequest(F95OptionId(""), F95OptionName("ignored"))
                )
            )
        ) as ParseResult.Success).value

        assertEquals(" Favorites ", parsed.name.value)
        assertEquals(F95Defaults.MAX_DATE_DAYS, parsed.dateLimit)
        assertEquals(listOf(FilterOptionRequest(F95OptionId("tag-1"), F95OptionName("tag-1"))), parsed.selectedTags)
    }

    @Test
    fun clampsDatesBelowAnyDateAndUsesEmptyOptionNamesAsIds() {
        val parsed = (parseSavedFilterResult(
            SavedFilter(
                name = F95OptionName("Recent"),
                dateLimit = Int.MIN_VALUE,
                excludedPrefixes = listOf(FilterOptionRequest(F95OptionId("prefix"), F95OptionName(" ")))
            )
        ) as ParseResult.Success).value

        assertEquals(F95Defaults.ANY_DATE, parsed.dateLimit)
        assertEquals("prefix", parsed.excludedPrefixes.single().name.value)
    }

    @Test
    fun truncatesOptionFieldsAndCapsTheCollection() {
        val options = (1..(f95gm.server.config.constants.FilterLimits.MAX_FILTER_OPTIONS + 5)).map { index ->
            FilterOptionRequest(
                F95OptionId("id-$index-" + "x".repeat(f95gm.server.config.constants.FilterLimits.OPTION_ID_LENGTH)),
                F95OptionName("name-$index-" + "y".repeat(f95gm.server.config.constants.FilterLimits.OPTION_NAME_LENGTH))
            )
        }

        val sanitized = sanitizeSavedFilterOptions(options)

        assertEquals(f95gm.server.config.constants.FilterLimits.MAX_FILTER_OPTIONS, sanitized.size)
        assertEquals(f95gm.server.config.constants.FilterLimits.OPTION_ID_LENGTH, sanitized.first().id.value.length)
        assertEquals(f95gm.server.config.constants.FilterLimits.OPTION_NAME_LENGTH, sanitized.first().name.value.length)
    }
}

