package f95gm.client.ui.shared.cards

internal data class GameCardData(
    val title: String,
    val developer: String,
    val cover: String,
    val prefixes: List<String>,
    val version: String,
    val date: String,
    val likes: String,
    val views: String,
    val rating: Double,
    val isNew: Boolean
)
