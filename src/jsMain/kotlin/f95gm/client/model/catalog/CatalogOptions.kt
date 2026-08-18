package f95gm.client.model.catalog

internal data class CatalogOptions(
    val tags: List<TagOption> = emptyList(),
    val prefixes: List<PrefixOption> = emptyList(),
    val prefixGroups: List<FilterGroup> = emptyList(),
    val loading: Boolean = false,
    val error: String = ""
)
