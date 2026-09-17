package f95gm.server.app

import f95gm.domain.api.openApiContractJson
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    val outputArgument = singleOutputArgument(args, "Usage: GenerateOpenApiContractKt <output>")
    kotlin.io.path.Path(outputArgument.outputOrThrow()).apply {
        createParentDirectories()
        writeText(openApiContractJson() + "\n")
    }
}
