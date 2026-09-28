package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class VerifyInstallerInputsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val installerInputs: ConfigurableFileCollection
    @TaskAction
    fun verify() {
        installerInputs.files.forEach { input ->
            check(input.isFile) { "Missing installer input: $input" }
            check(input.length() > 0L) { "Installer input must not be empty: $input" }
        }
        installerInputs.files.firstOrNull { it.name == "asset-manifest.json" }?.let { manifest ->
            val keys = Regex("\\\"([^\"]+)\\\":\\\"[0-9a-f]{64}\\\"")
                .findAll(manifest.readText())
                .map { it.groupValues[1] }
                .toList()
            check(keys == keys.sorted()) { "Asset manifest entries must be sorted for reproducible packaging" }
        }
    }
}

abstract class SmokePackagedLauncherTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val launcher: RegularFileProperty
    @get:Input @get:Optional abstract val runSmoke: Property<Boolean>

    @TaskAction
    fun verify() {
        val launcherFile = launcher.get().asFile
        check(launcherFile.isFile) { "Missing packaged launcher: $launcherFile" }
        val launcherText = launcherFile.readText()
        check(launcherText.contains("f95gm", ignoreCase = true) || launcherText.contains("packaging", ignoreCase = true)) {
            "Packaged launcher does not reference the application"
        }
        if (!runSmoke.getOrElse(false)) return
        val port = ServerSocket(0).use { it.localPort }
        val options = listOf(System.getenv("JAVA_TOOL_OPTIONS"), "-Df95gm.host=127.0.0.1", "-Df95gm.port=$port", "-Df95gm.openBrowser=false")
            .filterNot { it.isNullOrBlank() }.joinToString(" ")
        val command = if (launcherFile.extension.equals("bat", ignoreCase = true)) listOf("cmd.exe", "/c", launcherFile.absolutePath)
        else listOf(launcherFile.absolutePath)
        val process = ProcessBuilder(command).redirectErrorStream(true).apply { environment()["JAVA_TOOL_OPTIONS"] = options }.start()
        try {
            awaitPackagedHealth(process, port)
        } finally {
            process.toHandle().descendants().forEach { it.destroyForcibly() }
            process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
        }
    }
}

private fun awaitPackagedHealth(process: Process, port: Int) {
    val healthUrl = URI("http://127.0.0.1:$port/api/health?tabId=packaged-smoke").toURL()
    val healthStatus = CompletableFuture<Int>()
    val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "f95gm-packaged-health-probe").apply { isDaemon = true } }
    val probe = executor.scheduleAtFixedRate({
        if (healthStatus.isDone) return@scheduleAtFixedRate
        if (process.waitFor(0, TimeUnit.MILLISECONDS)) {
            healthStatus.complete(-1)
            return@scheduleAtFixedRate
        }
        try {
            val connection = healthUrl.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 500
                connection.readTimeout = 500
                connection.requestMethod = "GET"
                if (connection.responseCode == 200) healthStatus.complete(200)
            } finally { connection.disconnect() }
        } catch (_: ConnectException) {
            // Server is still starting or has not bound its port.
        } catch (_: SocketTimeoutException) {
            // Server is still starting or has not answered yet.
        } catch (error: IOException) {
            healthStatus.completeExceptionally(error)
        }
    }, 0, 100, TimeUnit.MILLISECONDS)
    try {
        val status = healthStatus.get(15, TimeUnit.SECONDS)
        if (status != 200) {
            val output = process.inputStream.bufferedReader().readText().takeLast(4_000)
            error("Packaged launcher exited with ${process.exitValue()} before health became ready${if (output.isBlank()) "" else ": $output"}")
        }
    } finally {
        probe.cancel(true)
        executor.shutdownNow()
    }
}
