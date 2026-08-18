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
