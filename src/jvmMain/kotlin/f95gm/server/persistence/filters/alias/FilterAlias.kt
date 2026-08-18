package f95gm.server.persistence.filters.alias

import f95gm.server.config.constants.ServerConstants
import java.text.Normalizer

internal fun filterAlias(name: String): String {
    val normalized = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFKD)
    val alias = buildString {
        var dashPending = false
        normalized.forEach { character ->
            val transliteration = FilterAliasRules.transliterations[character]
            when {
                character in 'a'..'z' || character in '0'..'9' -> {
                    if (dashPending && isNotEmpty()) append('-')
                    append(character)
                    dashPending = false
                }

                transliteration != null -> transliteration.forEach { transliterated ->
                    if (dashPending && isNotEmpty()) append('-')
                    append(transliterated)
                    dashPending = false
                }

                character.isWhitespace() || character in "-_." ||
                        character.category in SYMBOL_CATEGORIES || character.category in PUNCTUATION_CATEGORIES ->
                    dashPending = true

                character.category in COMBINING_MARK_CATEGORIES -> Unit
                // Unmapped letters are omitted so the result remains Latin-only.
            }
        }
    }.take(ServerConstants.FILTER_ID_LENGTH).trim('-')

    return alias.ifBlank { FILTER_ALIAS_FALLBACK }
}
