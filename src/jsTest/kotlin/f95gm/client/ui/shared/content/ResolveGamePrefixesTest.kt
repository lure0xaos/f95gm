package f95gm.client.ui.shared.content

import f95gm.client.model.catalog.PrefixOption
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95PrefixId
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveGamePrefixesTest {
    @Test
    fun resolvesKnownPrefixesAndDecodesTheirLabels() {
        val values = listOf(F95PrefixId("1"), F95PrefixId("2"))
        val prefixes = listOf(
            PrefixOption(F95PrefixId("1"), F95OptionName("3D &amp; Adult")),
            PrefixOption(F95PrefixId("2"), F95OptionName("Anime"))
        )

        assertEquals(listOf("3D & Adult", "Anime"), resolveGamePrefixes(values, prefixes))
    }

    @Test
    fun preservesNonNumericUnknownPrefixesButDropsUnknownNumericIds() {
        val values = listOf(F95PrefixId("custom"), F95PrefixId("99"), F95PrefixId(""))

        assertEquals(listOf("custom"), resolveGamePrefixes(values, emptyList()))
    }
}
