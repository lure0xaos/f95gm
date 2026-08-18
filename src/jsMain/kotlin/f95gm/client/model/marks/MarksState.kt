package f95gm.client.model.marks

import f95gm.domain.defaults.F95Defaults

internal data class MarksState(
    val items: List<MarkedGame> = emptyList(),
    val page: Int = F95Defaults.FIRST_PAGE,
    val editingPage: Boolean = false,
    val pageInput: String = F95Defaults.EMPTY,
    val sort: String = F95Defaults.DEFAULT_SORT,
    val search: String = F95Defaults.EMPTY,
    val loading: Boolean = false,
    val checking: Boolean = false,
    val error: String = ""
)
