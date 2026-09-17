package f95gm.server.persistence.database

import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.*

/** Restores one backup generation without exposing a partially copied database file. */
internal fun restoreDatabaseBackup(backupFile: Path, databaseFile: Path, overwrite: Boolean = false) {
    require(backupFile.isRegularFile()) { "Database backup does not exist: $backupFile" }
    require(overwrite || !databaseFile.exists()) { "Refusing to overwrite existing database: $databaseFile" }
    (databaseFile.parent ?: kotlin.io.path.Path(".")).createDirectories()
    val temporary = createTempFile(databaseFile.parent, databaseFile.fileName.toString(), ".restore.tmp")
    try {
        backupFile.copyTo(temporary, overwrite = true)
        if (overwrite) temporary.moveTo(databaseFile, ATOMIC_MOVE, REPLACE_EXISTING)
        else temporary.moveTo(databaseFile, ATOMIC_MOVE)
    } finally {
        temporary.deleteIfExists()
    }
}

fun main(args: Array<String>) {
    require(args.size == 2 || args.size == 3) { "Usage: RestoreDatabaseKt <backup> <database> [--overwrite]" }
    restoreDatabaseBackup(kotlin.io.path.Path(args[0]), kotlin.io.path.Path(args[1]), args.getOrNull(2) == "--overwrite")
}
