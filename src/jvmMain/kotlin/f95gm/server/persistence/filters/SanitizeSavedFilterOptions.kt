package f95gm.server.persistence.filters

import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.server.config.constants.ServerConstants
import f95gm.server.model.filters.SavedFilterOption

internal fun sanitizeSavedFilterOptions(options: List<SavedFilterOption>): List<SavedFilterOption> =
    options.mapNotNull { option ->
        val id = option.id.value.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        SavedFilterOption(
            F95OptionId(id.take(ServerConstants.OPTION_ID_LENGTH)),
            F95OptionName(option.name.value.trim().ifBlank { id }.take(ServerConstants.OPTION_NAME_LENGTH))
        )
    }.distinctBy { it.id }.take(ServerConstants.MAX_FILTER_OPTIONS)
