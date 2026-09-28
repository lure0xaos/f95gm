package f95gm.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class VerifyJavaRuntimeTask : DefaultTask() {
    @TaskAction
    fun verify() {
        val current = Runtime.version().feature()
        check(current >= 17) {
            "F95GM requires Java 17 or newer; detected Java $current. Set JAVA_HOME to a JDK 17+ installation."
        }
    }
}
