package f95gm.server.persistence.database

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Restores one backup generation without exposing a partially copied database file. */
internal fun restoreDatabaseBackup(backupFile: Path, databaseFile: Path, overwrite: Boolean = false) {
    require(Files.isRegularFile(backupFile)) { "Database backup does not exist: $backupFile" }
    require(overwrite || !Files.exists(databaseFile)) { "Refusing to overwrite existing database: $databaseFile" }
    Files.createDirectories(databaseFile.parent ?: Path.of("."))
    val temporary = Files.createTempFile(databaseFile.parent, databaseFile.fileName.toString(), ".restore.tmp")
    try {
        Files.copy(backupFile, temporary, REPLACE_EXISTING)
        if (overwrite) Files.move(temporary, databaseFile, ATOMIC_MOVE, REPLACE_EXISTING)
        else Files.move(temporary, databaseFile, ATOMIC_MOVE)
    } finally {
        Files.deleteIfExists(temporary)
    }
}

fun main(args: Array<String>) {
    require(args.size == 2 || args.size == 3) { "Usage: RestoreDatabaseKt <backup> <database> [--overwrite]" }
    restoreDatabaseBackup(Path.of(args[0]), Path.of(args[1]), args.getOrNull(2) == "--overwrite")
}
