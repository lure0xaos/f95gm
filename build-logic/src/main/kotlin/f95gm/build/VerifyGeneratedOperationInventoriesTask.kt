package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Ensures every generated client operation is represented by the generated server inventory. */
abstract class VerifyGeneratedOperationInventoriesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val serverRegistry: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val frontendRegistry: RegularFileProperty

    @TaskAction
    fun verify() {
        val server = operationIds(serverRegistry.get().asFile.readText())
        val frontend = operationIds(frontendRegistry.get().asFile.readText())
        check(server == frontend) {
            "Generated server/frontend operation inventories diverge: " +
                "serverOnly=${server - frontend}, frontendOnly=${frontend - server}"
        }
    }

    private fun operationIds(source: String): Set<String> =
        Regex("""ApiOperationId\s*\(\s*"([^"]+)"\s*\)""")
            .findAll(kotlinCode(source))
            .map { it.groupValues[1] }
            .toSet()
}
