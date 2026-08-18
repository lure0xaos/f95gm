package f95gm.server.routes.catalog.parsing

internal fun isJavaScriptIdentifierCharacterAfter(script: String, index: Int): Boolean =
    index < script.length && isJavaScriptIdentifierCharacter(script[index])
