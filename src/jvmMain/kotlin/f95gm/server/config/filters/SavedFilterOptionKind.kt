package f95gm.server.config.filters

/** Stable discriminator keys stored with saved-filter options. */
internal enum class SavedFilterOptionKind(val storageKey: String) {
    SELECTED_TAGS("selectedTags"),
    EXCLUDED_TAGS("excludedTags"),
    SELECTED_PREFIXES("selectedPrefixes"),
    EXCLUDED_PREFIXES("excludedPrefixes")
}
