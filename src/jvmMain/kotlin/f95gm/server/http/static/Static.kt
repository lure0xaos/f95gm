package f95gm.server.http.static

import f95gm.messages.UiMessages
import f95gm.server.config.constants.ServerConstants
import f95gm.server.http.response.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

internal suspend fun static(call: ApplicationCall, requestedPath: String) {
    val requested = requestedPath.ifBlank { "index.html" }
    if (requested.split('/').any { it == ".." }) {
        respond(
            call,
            ServerConstants.STATUS_NOT_FOUND,
            UiMessages.server_resourceNotFound(),
            ContentType.Text.Plain.toString()
        )
        return
    }
    val overrideRoot = System.getProperty("f95gm.staticDir")?.let {
        runCatching { Path.of(it).toRealPath() }.getOrNull()
    }
    val overrideFile = overrideRoot?.let { root ->
        runCatching { root.resolve(requested).toRealPath() }.getOrNull()
            ?.takeIf { file -> file.startsWith(root) && file.isRegularFile() }
    }
    val bytes = overrideFile?.readBytes() ?: Thread.currentThread().contextClassLoader
        .getResourceAsStream("static/$requested")
        ?.use { it.readBytes() }
    if (bytes == null) {
        respond(
            call,
            ServerConstants.STATUS_NOT_FOUND,
            UiMessages.server_resourceNotFound(),
            ContentType.Text.Plain.toString()
        )
        return
    }
    call.respondBytes(bytes, contentType = staticContentType(requested.substringAfterLast('.', "")))
}
