package f95gm.client.ui.detail.metadata

import f95gm.client.model.detail.DetailState

internal fun detailGenres(state: DetailState): List<String> = detailGenreValue(state)
    .split(',', ';', '|')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
