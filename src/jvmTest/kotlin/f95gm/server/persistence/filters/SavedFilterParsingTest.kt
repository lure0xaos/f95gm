package f95gm.server.persistence.filters

import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.filters.SavedFilter
import f95gm.server.model.filters.SavedFilterOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedFilterParsingTest {
    @Test
    fun rejectsBlankNamesAndNormalizesOptionsAndDateRange() {
        assertNull(parseSavedFilter(SavedFilter(name = F95OptionName(" "))))

        val parsed = parseSavedFilter(
            SavedFilter(
                id = F95FilterId("filter"),
                name = F95OptionName(" Favorites "),
                dateLimit = Int.MAX_VALUE,
                selectedTags = listOf(
                    SavedFilterOption(F95OptionId(" tag-1 "), F95OptionName("  ")),
                    SavedFilterOption(F95OptionId("tag-1"), F95OptionName("duplicate")),
                    SavedFilterOption(F95OptionId(""), F95OptionName("ignored"))
                )
            )
        )!!

        assertEquals(" Favorites ", parsed.name.value)
        assertEquals(F95Defaults.MAX_DATE_DAYS, parsed.dateLimit)
        assertEquals(listOf(SavedFilterOption(F95OptionId("tag-1"), F95OptionName("tag-1"))), parsed.selectedTags)
    }

    @Test
    fun clampsDatesBelowAnyDateAndUsesEmptyOptionNamesAsIds() {
        val parsed = parseSavedFilter(
            SavedFilter(
                name = F95OptionName("Recent"),
                dateLimit = Int.MIN_VALUE,
                excludedPrefixes = listOf(SavedFilterOption(F95OptionId("prefix"), F95OptionName(" ")))
            )
        )!!

        assertEquals(F95Defaults.ANY_DATE, parsed.dateLimit)
        assertEquals("prefix", parsed.excludedPrefixes.single().name.value)
    }

    @Test
    fun truncatesOptionFieldsAndCapsTheCollection() {
        val options = (1..(ServerConstants.MAX_FILTER_OPTIONS + 5)).map { index ->
            SavedFilterOption(
                F95OptionId("id-$index-" + "x".repeat(ServerConstants.OPTION_ID_LENGTH)),
                F95OptionName("name-$index-" + "y".repeat(ServerConstants.OPTION_NAME_LENGTH))
            )
        }

        val sanitized = sanitizeSavedFilterOptions(options)

        assertEquals(ServerConstants.MAX_FILTER_OPTIONS, sanitized.size)
        assertEquals(ServerConstants.OPTION_ID_LENGTH, sanitized.first().id.value.length)
        assertEquals(ServerConstants.OPTION_NAME_LENGTH, sanitized.first().name.value.length)
    }
}
