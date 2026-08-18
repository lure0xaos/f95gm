package f95gm.client.model.detail

internal data class DetailBodyState(
    val overviewHtml: String,
    val sections: List<DetailSection>,
    val fallbackHtml: String
)
