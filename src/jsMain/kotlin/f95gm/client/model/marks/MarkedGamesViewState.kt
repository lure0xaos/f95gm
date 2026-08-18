package f95gm.client.model.marks

internal data class MarkedGamesView(
    val items: List<MarkedGame>,
    val page: Int,
    val totalPages: Int
)
