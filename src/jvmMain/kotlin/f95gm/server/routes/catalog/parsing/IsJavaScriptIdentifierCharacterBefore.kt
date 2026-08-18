package f95gm.server.routes.catalog.parsing

internal fun isJavaScriptIdentifierCharacterBefore(script: String, index: Int): Boolean =
    index > 0 && isJavaScriptIdentifierCharacter(script[index - 1])
