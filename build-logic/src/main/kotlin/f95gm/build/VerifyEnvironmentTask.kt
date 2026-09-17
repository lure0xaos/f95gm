package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.work.DisableCachingByDefault
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Reports the local toolchain inputs that commonly make verification fail before compilation starts. */
@DisableCachingByDefault(because = "Reports live build-tool environment state")
abstract class VerifyEnvironmentTask : DefaultTask() {
    @get:Input
    abstract val expectedGradleVersion: Property<String>

    @TaskAction
    fun verify() {
        val java = Runtime.version().feature()
        val javaHome = System.getProperty("java.home")
        val node = commandVersion("node", "--version")
        val npm = commandVersion("npm", "--version")
        val databaseUrl = System.getenv("F95GM_DATABASE_URL")?.let { "configured" } ?: "not configured"

        logger.lifecycle("F95GM environment")
        logger.lifecycle("  Gradle: ${expectedGradleVersion.get()}")
        logger.lifecycle("  Java: $java (home=$javaHome)")
        logger.lifecycle("  JAVA_HOME: ${System.getenv("JAVA_HOME") ?: "not set"}")
        logger.lifecycle("  Node: ${node ?: "not found"}")
        logger.lifecycle("  npm: ${npm ?: "not found"}")
        logger.lifecycle("  F95GM_DATABASE_URL: $databaseUrl")

        check(java >= 17) {
            "F95GM requires Java 17 or newer; detected Java $java. Set JAVA_HOME to a JDK 17+ installation."
        }
    }

    private fun commandVersion(command: String, argument: String): String? = runCatching {
        ProcessBuilder(command, argument)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                process.inputStream.bufferedReader().use { it.readLine()?.trim() }.also { process.waitFor() }
            }
    }.getOrNull()?.takeIf(String::isNotBlank)
}
