package f95gm.server.config.session

internal fun isCookieNameCharacter(character: Char): Boolean =
    character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' || character == '_'
