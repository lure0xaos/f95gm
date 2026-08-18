package f95gm.client.model.filters

internal data class SavedFiltersState(
    val items: List<SavedFilter> = emptyList(),
    val loading: Boolean = false,
    val error: String = ""
)
