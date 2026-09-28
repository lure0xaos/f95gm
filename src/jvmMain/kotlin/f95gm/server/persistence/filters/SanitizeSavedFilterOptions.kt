package f95gm.server.persistence.filters

import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.domain.api.FilterOptionRequest

internal fun sanitizeSavedFilterOptions(options: List<FilterOptionRequest>): List<FilterOptionRequest> =
    options.mapNotNull { option ->
        val id = option.id.value.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        FilterOptionRequest(
            F95OptionId(id.take(f95gm.server.config.constants.FilterLimits.OPTION_ID_LENGTH)),
            F95OptionName(option.name.value.trim().ifBlank { id }.take(f95gm.server.config.constants.FilterLimits.OPTION_NAME_LENGTH))
        )
    }.distinctBy { it.id }.take(f95gm.server.config.constants.FilterLimits.MAX_FILTER_OPTIONS)

