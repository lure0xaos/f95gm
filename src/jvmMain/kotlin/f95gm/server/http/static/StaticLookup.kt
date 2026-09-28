package f95gm.server.http.static

import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal sealed interface StaticFileResolution {
    data class Found(val path: Path) : StaticFileResolution
    data object Missing : StaticFileResolution
    data class Failed(val failure: StaticFileFailure) : StaticFileResolution
}

internal enum class StaticFileFailure { INVALID_PATH, IO, SECURITY }

internal interface StaticFileSystem {
    fun resolveRoot(directory: String): StaticFileResolution
    fun resolveFile(root: Path, requested: String): StaticFileResolution
}

internal object NioStaticFileSystem : StaticFileSystem {
    override fun resolveRoot(directory: String): StaticFileResolution = try {
        StaticFileResolution.Found(Path.of(directory).toRealPath())
    } catch (_: InvalidPathException) {
        StaticFileResolution.Failed(StaticFileFailure.INVALID_PATH)
    } catch (_: NoSuchFileException) {
        StaticFileResolution.Missing
    } catch (_: NotDirectoryException) {
        StaticFileResolution.Missing
    } catch (_: IOException) {
        StaticFileResolution.Failed(StaticFileFailure.IO)
    } catch (_: SecurityException) {
        StaticFileResolution.Failed(StaticFileFailure.SECURITY)
    }

    override fun resolveFile(root: Path, requested: String): StaticFileResolution = try {
        val file = root.resolve(requested).normalize()
            .takeIf { candidate -> candidate.startsWith(root) }
            ?.toRealPath()
            ?.takeIf { candidate -> candidate.startsWith(root) && candidate.isRegularFile() }
        file?.let(StaticFileResolution::Found) ?: StaticFileResolution.Missing
    } catch (_: InvalidPathException) {
        StaticFileResolution.Failed(StaticFileFailure.INVALID_PATH)
    } catch (_: NoSuchFileException) {
        StaticFileResolution.Missing
    } catch (_: NotDirectoryException) {
        StaticFileResolution.Missing
    } catch (_: IOException) {
        StaticFileResolution.Failed(StaticFileFailure.IO)
    } catch (_: SecurityException) {
        StaticFileResolution.Failed(StaticFileFailure.SECURITY)
    }
}

internal fun resolveOverrideRoot(directory: String, fileSystem: StaticFileSystem = NioStaticFileSystem): StaticFileResolution =
    fileSystem.resolveRoot(directory)

internal fun resolveOverrideFile(root: Path, requested: String, fileSystem: StaticFileSystem = NioStaticFileSystem): StaticFileResolution =
    fileSystem.resolveFile(root, requested)
