package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.TimeUnit

abstract class BrowserE2eTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val launcher: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val e2eDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val databaseDirectory: DirectoryProperty

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val upstreamPort: Property<Int>

    @get:Input
    abstract val timeoutSeconds: Property<Long>

    @get:Input
    abstract val npmExecutable: Property<String>

    @TaskAction
    fun runBrowserE2e() {
        val launcherFile = launcher.get().asFile
        check(launcherFile.isFile) { "Missing packaged application launcher: $launcherFile" }
        val nodeVersion = runAndCapture(listOf("node", "--version"), e2eDirectory.get().asFile)
        val nodeMajor = Regex("v(\\d+)").find(nodeVersion)?.groupValues?.get(1)?.toIntOrNull()
        check(nodeMajor != null && nodeMajor >= 20) {
            "Browser E2E requires Node.js 20 or newer; found $nodeVersion"
        }

        val npm = npmExecutable.get()
        val database = databaseDirectory.get().asFile.resolve("f95gm")
        val fixtureKeyStore = prepareFixtureKeyStore(databaseDirectory.get().asFile)
        val browserLog = databaseDirectory.get().asFile.resolve("playwright.log")
        val browserTest = ProcessBuilder(npm, "--no-install", "playwright", "test")
            .directory(e2eDirectory.get().asFile)
            .redirectErrorStream(true)
            .apply {
                environment()["F95GM_E2E_URL"] = "http://127.0.0.1:${port.get()}"
                environment()["F95GM_E2E_COMMAND"] = launcherFile.absolutePath
                environment()["F95GM_MOCK_UPSTREAM_PORT"] = upstreamPort.get().toString()
                environment()["F95GM_MOCK_UPSTREAM_URL"] = "https://localhost:${upstreamPort.get()}"
                environment()["F95GM_MOCK_UPSTREAM_KEYSTORE"] = fixtureKeyStore.absolutePath
                environment()["JAVA_TOOL_OPTIONS"] =
                    "-Df95gm.host=127.0.0.1 -Df95gm.port=${port.get()} -Df95gm.openBrowser=false " +
                        "-Df95gm.database=${database.absolutePath} -Df95gm.upstreamBaseUrl=https://localhost:${upstreamPort.get()} " +
                        "-Df95gm.retryMaxAttempts=1 -Df95gm.retryBaseDelayMillis=1 -Df95gm.retryMaxDelayMillis=1 " +
                        "-Djavax.net.ssl.trustStore=\"${fixtureKeyStore.absolutePath}\" " +
                        "-Djavax.net.ssl.trustStorePassword=local-fixture-password -Djavax.net.ssl.trustStoreType=PKCS12"
            }
            .redirectOutput(browserLog)
            .start()
        try {
            check(browserTest.waitFor(timeoutSeconds.get(), TimeUnit.SECONDS)) {
                browserTest.destroyForcibly()
                "Browser E2E timed out after ${timeoutSeconds.get()} seconds"
            }
            logger.lifecycle(browserLog.readText())
            check(browserTest.exitValue() == 0) { "Browser E2E smoke test failed; see ${browserLog.absolutePath}" }
        } finally {
            if (browserTest.isAlive) {
                browserTest.destroy()
                if (!browserTest.waitFor(5, TimeUnit.SECONDS)) browserTest.destroyForcibly()
            }
        }
    }

    /** Test-only TLS identity and trust store keep the production HTTPS policy intact. */
    private fun prepareFixtureKeyStore(directory: java.io.File): java.io.File {
        directory.mkdirs()
        val keyStore = directory.resolve("mock-upstream.p12")
        if (!keyStore.isFile) {
            val windows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
            val keytool = java.io.File(System.getProperty("java.home"), "bin/keytool${if (windows) ".exe" else ""}")
            runAndCapture(listOf(
                keytool.absolutePath, "-genkeypair", "-alias", "mock-upstream", "-keyalg", "RSA",
                "-storetype", "PKCS12", "-keystore", keyStore.absolutePath,
                "-storepass", "local-fixture-password", "-keypass", "local-fixture-password",
                "-dname", "CN=localhost", "-ext", "SAN=ip:127.0.0.1,dns:localhost", "-validity", "3650"
            ), directory)
        }
        return keyStore
    }

    private fun runAndCapture(command: List<String>, directory: java.io.File): String {
        val process = ProcessBuilder(command)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) {
            "Unable to run ${command.first()}: $output"
        }
        return output
    }
}
