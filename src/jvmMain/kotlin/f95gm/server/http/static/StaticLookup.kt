package f95gm.server.http.static

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.SecureDirectoryStream
import java.nio.file.StandardOpenOption.READ
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.nio.channels.SeekableByteChannel
import java.io.ByteArrayOutputStream
import kotlin.io.path.isRegularFile

internal sealed interface StaticFileResolution {
    data class Found(val path: Path) : StaticFileResolution
    data object Missing : StaticFileResolution
    data class Failed(val failure: StaticFileFailure) : StaticFileResolution
}

internal sealed interface StaticFileOpenResult {
    data class Opened(val file: StaticFileAccess) : StaticFileOpenResult
    data object Missing : StaticFileOpenResult
    data class Failed(val failure: StaticFileFailure) : StaticFileOpenResult
}

internal interface StaticFileAccess : AutoCloseable {
    val path: Path
    val size: Long
    val modifiedAt: FileTime
    fun readBytes(): ByteArray
}

internal enum class StaticFileFailure { INVALID_PATH, IO, SECURITY }

internal interface StaticFileSystem {
    fun resolveRoot(directory: String): StaticFileResolution
    fun resolveFile(root: Path, requested: String): StaticFileResolution
    fun openFile(root: Path, requested: String): StaticFileOpenResult =
        when (val resolution = resolveFile(root, requested)) {
            is StaticFileResolution.Found -> NioStaticFileSystem.openResolvedFile(root, resolution.path)
            StaticFileResolution.Missing -> StaticFileOpenResult.Missing
            is StaticFileResolution.Failed -> StaticFileOpenResult.Failed(resolution.failure)
        }
}

internal object NioStaticFileSystem : StaticFileSystem {
    override fun resolveRoot(directory: String): StaticFileResolution = try {
        StaticFileResolution.Found(kotlin.io.path.Path(directory).toRealPath())
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

    override fun openFile(root: Path, requested: String): StaticFileOpenResult = try {
        val relative = kotlin.io.path.Path(requested).normalize()
        if (relative.isAbsolute || relative.startsWith("..")) return StaticFileOpenResult.Missing
        val rootStream = java.nio.file.Files.newDirectoryStream(root)
        if (rootStream is SecureDirectoryStream<Path>) {
            openSecureFile(root, relative, rootStream)
        } else {
            rootStream.close()
            val candidate = root.resolve(relative).normalize()
                .takeIf { it.startsWith(root) }
                ?.toRealPath()
                ?.takeIf { it.startsWith(root) && it.isRegularFile() }
                ?: return StaticFileOpenResult.Missing
            openResolvedFile(root, candidate)
        }
    } catch (_: InvalidPathException) {
        StaticFileOpenResult.Failed(StaticFileFailure.INVALID_PATH)
    } catch (_: NoSuchFileException) {
        StaticFileOpenResult.Missing
    } catch (_: NotDirectoryException) {
        StaticFileOpenResult.Missing
    } catch (_: IOException) {
        StaticFileOpenResult.Failed(StaticFileFailure.IO)
    } catch (_: SecurityException) {
        StaticFileOpenResult.Failed(StaticFileFailure.SECURITY)
    }

    internal fun openResolvedFile(root: Path, file: Path): StaticFileOpenResult = try {
        if (!file.toRealPath().startsWith(root.toRealPath())) return StaticFileOpenResult.Missing
        val attributes = java.nio.file.Files.readAttributes(
            file,
            java.nio.file.attribute.BasicFileAttributes::class.java,
            LinkOption.NOFOLLOW_LINKS
        )
        if (!attributes.isRegularFile) return StaticFileOpenResult.Missing
        val channel = FileChannel.open(file, READ, LinkOption.NOFOLLOW_LINKS)
        if (!file.toRealPath().startsWith(root.toRealPath())) {
            channel.close()
            return StaticFileOpenResult.Missing
        }
        StaticFileOpenResult.Opened(NioStaticFileAccess(file, attributes.size(), attributes.lastModifiedTime(), channel))
    } catch (_: InvalidPathException) {
        StaticFileOpenResult.Failed(StaticFileFailure.INVALID_PATH)
    } catch (_: NoSuchFileException) {
        StaticFileOpenResult.Missing
    } catch (_: NotDirectoryException) {
        StaticFileOpenResult.Missing
    } catch (_: IOException) {
        StaticFileOpenResult.Failed(StaticFileFailure.IO)
    } catch (_: SecurityException) {
        StaticFileOpenResult.Failed(StaticFileFailure.SECURITY)
    }

    private fun openSecureFile(
        root: Path,
        relative: Path,
        rootStream: SecureDirectoryStream<Path>
    ): StaticFileOpenResult {
        val streams = mutableListOf<SecureDirectoryStream<Path>>(rootStream)
        var channel: SeekableByteChannel? = null
        var ownershipTransferred = false
        try {
            var current = rootStream
            val components = relative.map { it.toString() }
            if (components.isEmpty()) return StaticFileOpenResult.Missing
            components.dropLast(1).forEach { component ->
                current = current.newDirectoryStream(kotlin.io.path.Path(component), LinkOption.NOFOLLOW_LINKS)
                streams += current
            }
            val name = kotlin.io.path.Path(components.last())
            val attributes = current.getFileAttributeView(
                name,
                BasicFileAttributeView::class.java,
                LinkOption.NOFOLLOW_LINKS
            )?.readAttributes() ?: return StaticFileOpenResult.Missing
            if (!attributes.isRegularFile) return StaticFileOpenResult.Missing
            channel = current.newByteChannel(name, setOf<OpenOption>(READ, LinkOption.NOFOLLOW_LINKS))
            val canonicalRoot = root.toRealPath()
            val canonicalFile = root.resolve(relative).normalize().toRealPath()
            if (!canonicalFile.startsWith(canonicalRoot)) {
                channel.close()
                channel = null
                return StaticFileOpenResult.Missing
            }
            val access = NioStaticFileAccess(
                canonicalFile,
                attributes.size(),
                attributes.lastModifiedTime(),
                channel,
                streams
            )
            ownershipTransferred = true
            channel = null
            return StaticFileOpenResult.Opened(access)
        } catch (error: IOException) {
            if (error is NoSuchFileException || error is NotDirectoryException) return StaticFileOpenResult.Missing
            return StaticFileOpenResult.Failed(StaticFileFailure.IO)
        } catch (_: SecurityException) {
            return StaticFileOpenResult.Failed(StaticFileFailure.SECURITY)
        } catch (_: InvalidPathException) {
            return StaticFileOpenResult.Failed(StaticFileFailure.INVALID_PATH)
        } finally {
            if (!ownershipTransferred) {
                channel?.close()
                streams.asReversed().forEach { it.close() }
            }
        }
    }
}

private class NioStaticFileAccess(
    override val path: Path,
    override val size: Long,
    override val modifiedAt: FileTime,
    private val channel: SeekableByteChannel,
    private val streams: List<SecureDirectoryStream<Path>> = emptyList()
) : StaticFileAccess {
    override fun readBytes(): ByteArray {
        channel.position(0)
        val output = ByteArrayOutputStream(size.coerceAtMost(8_192L).toInt())
        val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
        var read: Int
        do {
            read = channel.read(buffer)
            if (read > 0) {
                buffer.flip()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                output.write(bytes)
                buffer.clear()
            }
        } while (read >= 0)
        return output.toByteArray()
    }

    override fun close() {
        try {
            channel.close()
        } finally {
            streams.asReversed().forEach { it.close() }
        }
    }
}

internal fun resolveOverrideRoot(directory: String, fileSystem: StaticFileSystem = NioStaticFileSystem): StaticFileResolution =
    fileSystem.resolveRoot(directory)

internal fun resolveOverrideFile(root: Path, requested: String, fileSystem: StaticFileSystem = NioStaticFileSystem): StaticFileResolution =
    fileSystem.resolveFile(root, requested)
