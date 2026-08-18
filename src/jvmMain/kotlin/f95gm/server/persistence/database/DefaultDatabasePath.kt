package f95gm.server.persistence.database

import java.nio.file.Path

internal fun defaultDatabasePath(): String =
    Path.of(System.getProperty("user.home"), "F95GM", "f95gm").toString()
