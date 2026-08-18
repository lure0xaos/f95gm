package f95gm.client.data.catalog

import f95gm.client.model.catalog.Game

internal data class CatalogResult(val items: List<Game>, val totalPages: Int)
