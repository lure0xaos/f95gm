package f95gm.server.persistence.database

internal fun defaultDatabasePath(userHome: String): String =
    kotlin.io.path.Path(userHome, "F95GM", "f95gm").toString()
