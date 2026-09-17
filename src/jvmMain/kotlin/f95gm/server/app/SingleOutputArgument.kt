package f95gm.server.app

internal sealed interface SingleOutputArgument {
    data class Valid(val output: String) : SingleOutputArgument
    data class Invalid(val usage: String) : SingleOutputArgument
}

internal fun singleOutputArgument(args: Array<String>, usage: String): SingleOutputArgument =
    if (args.size == 1 && args[0].isNotBlank()) SingleOutputArgument.Valid(args[0])
    else SingleOutputArgument.Invalid(usage)

internal fun SingleOutputArgument.outputOrThrow(): String = when (this) {
    is SingleOutputArgument.Valid -> output
    is SingleOutputArgument.Invalid -> error(usage)
}
