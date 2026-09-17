package f95gm.server.app

import f95gm.server.persistence.database.CURRENT_SCHEMA_VERSION
import f95gm.server.persistence.database.migrationPlanReport
import f95gm.server.persistence.database.migrationRegistry
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/** Emits a deterministic migration plan without opening a database or executing SQL. */
fun main(args: Array<String>) {
    val outputArgument = singleOutputArgument(args, "Usage: migrationDryRun <output-file>")
    val report = migrationPlanReport(0, CURRENT_SCHEMA_VERSION, migrationRegistry)
    kotlin.io.path.Path(outputArgument.outputOrThrow()).apply {
        createParentDirectories()
        writeText(buildString {
            appendLine("from=${report.fromVersion.value}")
            appendLine("to=${report.toVersion.value}")
            appendLine("noOp=${report.isNoOp}")
            report.steps.forEach { step ->
                appendLine("${step.direction.name.lowercase()}|${step.metadata.version.value}|${step.metadata.description}|${step.metadata.checksum.value}")
            }
        })
    }
}
