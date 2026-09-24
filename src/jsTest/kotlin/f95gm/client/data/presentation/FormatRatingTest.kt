package f95gm.client.data.presentation

import f95gm.client.state.config.ClientConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatRatingTest {
    @Test
    fun formatsMissingRatingAsPlaceholder() {
        assertEquals("—", formatRating(ClientConstants.NO_RATING))
    }

    @Test
    fun formatsRatingsToOneDecimalByTruncatingScaledValue() {
        assertEquals("8.7", formatRating(8.79))
        assertEquals("10", formatRating(10.0))
    }
}
