package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyToolchainConsistencyTask : DefaultTask() {
    @get:Input
    abstract val expectedJavaMajor: Property<Int>

    @get:Input
    abstract val expectedKotlinVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionCatalog: RegularFileProperty

    @TaskAction
    fun verify() {
        val javaMajor = Runtime.version().feature()
        check(javaMajor == expectedJavaMajor.get()) {
            "F95GM local and CI builds must use Java ${expectedJavaMajor.get()}; detected Java $javaMajor."
        }

        val buildText = buildScript.get().asFile.readText()
        check("jvmToolchain(libs.versions.java.get().toInt())" in buildText) {
            "F95GM Gradle Kotlin/JVM toolchain must use the shared Java version catalog entry."
        }

        val catalogText = versionCatalog.get().asFile.readText()
        check("kotlin = \"${expectedKotlinVersion.get()}\"" in catalogText) {
            "F95GM local and CI builds must use the pinned Kotlin ${expectedKotlinVersion.get()} toolchain."
        }
    }
}
