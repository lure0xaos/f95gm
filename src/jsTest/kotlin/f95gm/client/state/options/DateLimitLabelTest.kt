package f95gm.client.state.options

import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class DateLimitLabelTest {
    @Test
    fun resolvesKnownDateLimitsToLocalizedLabels() {
        assertEquals(UiMessages.app_anyTime(), dateLimitLabel(F95Defaults.ANY_DATE))
        assertEquals(UiMessages.app_today(), dateLimitLabel(F95Defaults.TODAY))
        assertEquals(UiMessages.app_lastWeek(), dateLimitLabel(F95Defaults.LAST_WEEK))
    }

    @Test
    fun fallsBackToAnyTimeForUnknownValues() {
        assertEquals(UiMessages.app_anyTime(), dateLimitLabel(-1))
        assertEquals(UiMessages.app_anyTime(), dateLimitLabel(Int.MAX_VALUE))
    }
}
