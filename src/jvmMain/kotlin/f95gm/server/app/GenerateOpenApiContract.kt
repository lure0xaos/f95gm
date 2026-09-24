package f95gm.server.app

import f95gm.domain.api.F95Api
import f95gm.domain.api.openApiContractJson
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    require(args.size == 1) { "Usage: GenerateOpenApiContractKt <output>" }
    val output = Path.of(args[0])
    output.parent.createDirectories()
    output.writeText(F95Api.openApiContractJson() + "\n")
}
