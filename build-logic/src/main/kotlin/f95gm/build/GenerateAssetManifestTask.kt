package f95gm.build

import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateAssetManifestTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assets: DirectoryProperty

    @get:OutputFile
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun generate() {
        val assetDirectory = assets.get().asFile
        val entries = assetDirectory.walkTopDown()
            .filter { it.isFile && it.name != manifest.get().asFile.name }
            .sortedBy { it.relativeTo(assetDirectory).invariantSeparatorsPath }
            .map { file ->
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(file.readBytes())
                    .joinToString("") { byte -> "%02x".format(byte) }
                "\"${file.relativeTo(assetDirectory).invariantSeparatorsPath}\":\"$digest\""
            }
            .toList()
        manifest.get().asFile.apply {
            parentFile.mkdirs()
            writeText("{${entries.joinToString(",")}}\n")
        }
    }
}
