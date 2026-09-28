package f95gm.server.persistence.database

import java.nio.file.Path

internal fun defaultDatabasePath(userHome: String): String =
    Path.of(userHome, "F95GM", "f95gm").toString()
