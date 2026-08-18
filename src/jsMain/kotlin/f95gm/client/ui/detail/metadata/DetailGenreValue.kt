package f95gm.client.ui.detail.metadata

import f95gm.client.model.detail.DetailState
import f95gm.messages.UiMessages

internal fun detailGenreValue(state: DetailState): String = state.meta
    .firstOrNull { it.label.value.equals(UiMessages.app_genre(), ignoreCase = true) }
    ?.value
    ?.value
    ?.takeIf { it.isNotBlank() }
    ?: state.sections
        .firstOrNull { it.title.value.equals(UiMessages.app_genre(), ignoreCase = true) }
        ?.html
        ?.value
        ?.let(::plainGenreText)
        .orEmpty()
