package f95gm.server.routes.catalog

internal fun isNumericId(value: String): Boolean = value.toIntOrNull()?.let { it > 0 } == true
