package f95gm.server.routes.catalog.parsing

internal fun isJavaScriptIdentifierCharacter(character: Char): Boolean =
    character.isLetterOrDigit() || character == '_' || character == '$'
