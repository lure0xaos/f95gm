package f95gm.server.http.static

import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal fun resolveOverrideRoot(directory: String): Path? = try {
    Path.of(directory).toRealPath()
} catch (_: InvalidPathException) {
    null
} catch (_: IOException) {
    null
} catch (_: SecurityException) {
    null
}

internal fun resolveOverrideFile(root: Path, requested: String): Path? = try {
    root.resolve(requested).toRealPath()
        .takeIf { file -> file.startsWith(root) && file.isRegularFile() }
} catch (_: InvalidPathException) {
    null
} catch (_: IOException) {
    null
} catch (_: SecurityException) {
    null
}
