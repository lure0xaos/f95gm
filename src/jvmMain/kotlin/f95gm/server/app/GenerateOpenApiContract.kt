package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.domain.api.openApiContractJson
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    require(args.size == 1) { "Usage: GenerateOpenApiContractKt <output>" }
    val output = Path.of(args[0])
    Files.createDirectories(output.parent)
    Files.writeString(output, F95Api.openApiContractJson() + "\n")
}
