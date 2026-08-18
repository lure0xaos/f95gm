package f95gm.client.ui.shared.content

import f95gm.client.model.catalog.PrefixOption
import f95gm.domain.values.F95PrefixId

internal fun resolveGamePrefixes(values: List<F95PrefixId>, prefixes: List<PrefixOption>): List<String> =
    values.mapNotNull { value ->
        prefixes.firstOrNull { it.id == value }?.name?.value
            ?: value.value.takeIf { it.isNotBlank() && it.toIntOrNull() == null }
    }.map(::decodeHtmlEntities)
