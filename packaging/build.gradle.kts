plugins {
    alias(libs.plugins.beryx.runtime)
}

dependencies {
    implementation(project(":"))
}

val iconPath: File = rootProject.layout.projectDirectory
    .file("src/jsMain/resources/F95GM.ico")
    .asFile

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
        installerType = "msi"
        appVersion = rootProject.version.toString()
        imageOptions = listOf("--icon", iconPath.absolutePath)
        installerOptions = listOf("--win-menu", "--win-shortcut")
        jvmArgs = listOf("-Df95gm.port=0")
    }
}

listOf("jre", "jpackageImage", "jpackage").forEach { taskName ->
    tasks.named(taskName) {
        notCompatibleWithConfigurationCache("Beryx Runtime accesses the project during packaging task execution.")
    }
}
