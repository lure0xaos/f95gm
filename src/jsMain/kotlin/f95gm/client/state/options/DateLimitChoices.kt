package f95gm.client.state.options

import f95gm.client.model.catalog.DropdownChoice
import f95gm.domain.defaults.F95Defaults
import f95gm.messages.UiMessages

internal val dateLimitChoices = listOf(
    DropdownChoice(F95Defaults.ANY_DATE.toString(), UiMessages.app_anyTime()),
    DropdownChoice(F95Defaults.TODAY.toString(), UiMessages.app_today()),
    DropdownChoice(F95Defaults.LAST_3_DAYS.toString(), UiMessages.app_last3Days()),
    DropdownChoice(F95Defaults.LAST_WEEK.toString(), UiMessages.app_lastWeek()),
    DropdownChoice(F95Defaults.LAST_2_WEEKS.toString(), UiMessages.app_last2Weeks()),
    DropdownChoice(F95Defaults.LAST_MONTH.toString(), UiMessages.app_lastMonth()),
    DropdownChoice(F95Defaults.LAST_3_MONTHS.toString(), UiMessages.app_last3Months()),
    DropdownChoice(F95Defaults.LAST_YEAR.toString(), UiMessages.app_lastYear())
)
