import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

abstract class VerifyInstallerInputsTask : DefaultTask() {
    @get:InputFiles
    abstract val installerInputs: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        installerInputs.files.forEach { input ->
            check(input.isFile) { "Missing installer input: $input" }
        }
    }
}

abstract class SmokePackagedLauncherTask : DefaultTask() {
    @get:InputFile
    abstract val launcher: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val runSmoke: Property<Boolean>

    @TaskAction
    fun verify() {
        val launcherFile = launcher.get().asFile
        check(launcherFile.isFile) { "Missing packaged launcher: $launcherFile" }
        check(
            launcherFile.readText().contains("f95gm", ignoreCase = true) || launcherFile.readText()
                .contains("packaging", ignoreCase = true)
        ) {
            "Packaged launcher does not reference the application"
        }
        if (runSmoke.getOrElse(false)) {
            val port = ServerSocket(0).use { it.localPort }
            val javaToolOptions = listOf(
                System.getenv("JAVA_TOOL_OPTIONS"),
                "-Df95gm.host=127.0.0.1",
                "-Df95gm.port=$port",
                "-Df95gm.openBrowser=false"
            ).filterNot { it.isNullOrBlank() }.joinToString(" ")
            val command = if (launcherFile.extension.equals("bat", ignoreCase = true)) {
                listOf("cmd.exe", "/c", launcherFile.absolutePath)
            } else {
                listOf(launcherFile.absolutePath)
            }
            val process = ProcessBuilder(command).redirectErrorStream(true).apply {
                environment()["JAVA_TOOL_OPTIONS"] = javaToolOptions
            }.start()
            try {
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
                var status: Int? = null
                while (System.nanoTime() < deadline && status == null) {
                    try {
                        status = (URI("http://127.0.0.1:$port/api/health?tabId=packaged-smoke").toURL()
                            .openConnection() as HttpURLConnection).run {
                            connectTimeout = 500
                            readTimeout = 500
                            requestMethod = "GET"
                            responseCode
                        }
                    } catch (_: Exception) {
                        Thread.sleep(100)
                    }
                    if (process.waitFor(0, TimeUnit.MILLISECONDS)) {
                        val output = process.inputStream.bufferedReader().readText().takeLast(4_000)
                        error("Packaged launcher exited with ${process.exitValue()} before health became ready${if (output.isBlank()) "" else ": $output"}")
                    }
                }
                check(status == 200) { "Packaged application did not become healthy on port $port" }
            } finally {
                process.toHandle().descendants().forEach { child -> child.destroyForcibly() }
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
            }
        }
    }
}

plugins {
    alias(libs.plugins.beryx.runtime)
}

dependencies {
    implementation(project(":"))
}

val iconPath: File = rootProject.layout.projectDirectory
    .file("src/jsMain/resources/F95GM.ico")
    .asFile
val iconPngPath: File = rootProject.layout.projectDirectory
    .file("src/jsMain/resources/F95GM.png")
    .asFile
val macIconPath: File = rootProject.layout.projectDirectory
    .file("src/jsMain/resources/F95GM.icns")
    .asFile
val hostOs = System.getProperty("os.name").lowercase()
val isWindows = hostOs.contains("win")
val isMacOs = hostOs.contains("mac")
val isLinux = hostOs.contains("nux")
check(isWindows || isMacOs || isLinux) {
    "Unsupported packaging host OS: ${System.getProperty("os.name")}"
}
val nativeInstallerType = when {
    isWindows -> "msi"
    isMacOs -> "dmg"
    else -> "deb"
}
application {
    mainClass.set("f95gm.server.entry.MainKt")
}

runtime {
    additive = true
    modules = listOf("java.desktop", "java.naming", "java.sql", "jdk.crypto.ec")

    jpackage {
        outputDir = "installer"
        imageName = rootProject.name
        installerName = rootProject.name
        installerType = nativeInstallerType
        appVersion = rootProject.version.toString()
        imageOptions = listOf(
            "--icon",
            when {
                isWindows -> iconPath.absolutePath
                isMacOs -> macIconPath.absolutePath
                else -> iconPngPath.absolutePath
            }
        )
        installerOptions = buildList {
            add("--vendor")
            add("F95 Game Manager")
            if (isWindows) {
                addAll(
                    listOf(
                        "--win-menu",
                        "--win-menu-group", "F95 Game Manager",
                        "--win-shortcut",
                        "--win-shortcut-prompt",
                        "--win-dir-chooser"
                    )
                )
            }
            if (isLinux) {
                addAll(
                    listOf(
                        "--linux-menu-group", "F95 Game Manager",
                        "--linux-shortcut"
                    )
                )
            }
        }
        jvmArgs = listOf("-Df95gm.port=0")
    }
}

listOf("jre", "jpackageImage", "jpackage").forEach { taskName ->
    tasks.named(taskName) {
        notCompatibleWithConfigurationCache("Beryx Runtime accesses the project during packaging task execution.")
    }
}


val verifyInstallerInputs = tasks.register<VerifyInstallerInputsTask>("verifyInstallerInputs") {
    group = "verification"
    description = "Verifies native installer inputs before invoking jpackage."
    installerInputs.from(
        when {
            isWindows -> iconPath
            isMacOs -> macIconPath
            else -> iconPngPath
        },
        rootProject.layout.buildDirectory.file("generated/jvmResources/main/static/asset-manifest.json")
    )
}

verifyInstallerInputs.configure {
    dependsOn(rootProject.tasks.named("copyJsToJvm"))
}

tasks.named("jpackage") { dependsOn(verifyInstallerInputs) }
tasks.named("check") { dependsOn(verifyInstallerInputs) }

val smokePackagedLauncher = tasks.register<SmokePackagedLauncherTask>("smokePackagedLauncher") {
    group = "verification"
    description = "Verifies that the packaged launcher exists and is executable by the host shell."
    dependsOn("installDist")
    launcher.set(layout.buildDirectory.file("install/packaging/bin/${if (isWindows) "packaging.bat" else "packaging"}"))
    runSmoke.set(providers.gradleProperty("runInstallerSmoke").map { true }.orElse(false))
}

tasks.named("check") { dependsOn(smokePackagedLauncher) }
