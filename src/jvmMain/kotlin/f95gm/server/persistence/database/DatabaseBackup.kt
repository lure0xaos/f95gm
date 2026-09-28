package f95gm.server.persistence.database

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.copyTo
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.moveTo

private const val DATABASE_BACKUP_GENERATIONS = 3

/** Stages the old chain so a failed promotion can restore every previously published generation. */
internal fun createDatabaseBackup(
    databaseFile: Path,
    backupFile: Path,
    move: (Path, Path) -> Unit = ::moveReplacing
) {
    val parent = backupFile.parent ?: databaseBackupInvariant("Backup file must have a parent directory")
    val stagedBackup = createTempFile(parent, "${backupFile.fileName}.", ".backup.tmp")
    val rotation = BackupRotation(backupFile, Files.createTempDirectory(parent, "${backupFile.fileName}.rotation."))
    var complete = false
    try {
        copyAndValidate(databaseFile, stagedBackup)
        rotation.stagePrevious(move)
        rotation.publishPrevious(move)
        rotation.promoteFresh(stagedBackup, move)
        if (backupFile.fileSize() != databaseFile.fileSize()) databaseBackupInvariant(
            "Database backup validation failed after atomic replacement"
        )
        complete = true
    } catch (failure: IOException) {
        rotation.restore(stagedBackup, move, failure)
        throw failure
    } catch (failure: DatabaseBackupInvariantViolation) {
        rotation.restore(stagedBackup, move, failure)
        throw failure
    } finally {
        stagedBackup.deleteIfExists()
        rotation.cleanup(complete)
    }
}

private fun copyAndValidate(databaseFile: Path, stagedBackup: Path) {
    databaseFile.copyTo(stagedBackup, overwrite = true)
    if (stagedBackup.fileSize() != databaseFile.fileSize()) databaseBackupInvariant(
        "Database backup size does not match the source database"
    )
}

private class BackupRotation(private val backupFile: Path, private val directory: Path) {
    private val staged = (0..DATABASE_BACKUP_GENERATIONS).associateWith { directory.resolve("generation-$it") }
    private val originals = (0..DATABASE_BACKUP_GENERATIONS).associateWith { generation ->
        if (generation == 0) backupFile else generationPath(backupFile, generation)
    }
    private val published = mutableListOf<Pair<Path, Path>>()
    private var freshPromotionAttempted = false

    fun stagePrevious(move: (Path, Path) -> Unit) {
        originals.forEach { (generation, original) ->
            if (original.exists()) move(original, staged.getValue(generation))
        }
    }

    fun publishPrevious(move: (Path, Path) -> Unit) {
        for (generation in 0 until DATABASE_BACKUP_GENERATIONS) {
            val source = staged.getValue(generation)
            if (source.exists()) {
                val target = generationPath(backupFile, generation + 1)
                published += source to target
                move(source, target)
            }
        }
    }

    fun promoteFresh(stagedBackup: Path, move: (Path, Path) -> Unit) {
        freshPromotionAttempted = true
        move(stagedBackup, backupFile)
    }

    fun restore(stagedBackup: Path, move: (Path, Path) -> Unit, failure: Throwable) {
        val rollbackFailures = mutableListOf<IOException>()
        if (freshPromotionAttempted && !stagedBackup.exists() && backupFile.exists()) {
            restoreMove(backupFile, stagedBackup, move, rollbackFailures)
        }
        published.asReversed().forEach { (source, target) ->
            if (!source.exists() && target.exists()) restoreMove(target, source, move, rollbackFailures)
        }
        originals.forEach { (generation, original) ->
            val source = staged.getValue(generation)
            if (source.exists()) restoreMove(source, original, move, rollbackFailures)
        }
        rollbackFailures.forEach(failure::addSuppressed)
    }

    fun cleanup(complete: Boolean) {
        if (complete) staged.values.forEach(Path::deleteIfExists)
        if (complete || staged.values.none(Path::exists)) Files.deleteIfExists(directory)
    }
}

private fun restoreMove(source: Path, target: Path, move: (Path, Path) -> Unit, failures: MutableList<IOException>) {
    try {
        move(source, target)
    } catch (failure: IOException) {
        failures += failure
    }
}

private fun databaseBackupInvariant(message: String): Nothing = throw DatabaseBackupInvariantViolation(message)

private fun generationPath(backupFile: Path, generation: Int): Path =
    backupFile.resolveSibling("${backupFile.fileName}.$generation")

private fun moveReplacing(source: Path, target: Path) {
    try {
        source.moveTo(target, ATOMIC_MOVE, REPLACE_EXISTING)
    } catch (_: AtomicMoveNotSupportedException) {
        source.moveTo(target, REPLACE_EXISTING)
    }
}
