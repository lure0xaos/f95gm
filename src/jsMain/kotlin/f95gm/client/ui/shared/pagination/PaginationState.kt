package f95gm.client.ui.shared.pagination


internal data class PaginationState(
    val page: Int,
    val totalPages: Int,
    val loading: Boolean,
    val editingPage: PageEditMode,
    val pageInput: String
)
