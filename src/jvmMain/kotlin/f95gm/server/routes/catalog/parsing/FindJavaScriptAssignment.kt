package f95gm.server.routes.catalog.parsing

internal fun findJavaScriptAssignment(script: String, variable: String): Int? {
    var searchFrom = 0
    while (searchFrom < script.length) {
        val variableStart = script.indexOf(variable, searchFrom, ignoreCase = true)
        if (variableStart == -1) return null
        val variableEnd = variableStart + variable.length
        if (!isJavaScriptIdentifierCharacterAfter(script, variableEnd)) {
            var beforeVariable = variableStart
            while (beforeVariable > 0 && script[beforeVariable - 1].isWhitespace()) beforeVariable--
            val hasWindowPrefix = beforeVariable >= WINDOW_PREFIX.length &&
                    script.regionMatches(
                        beforeVariable - WINDOW_PREFIX.length,
                        WINDOW_PREFIX,
                        0,
                        WINDOW_PREFIX.length,
                        ignoreCase = true
                    ) && !isJavaScriptIdentifierCharacterBefore(script, beforeVariable - WINDOW_PREFIX.length)
            val declarationEnd = beforeVariable
            var declarationStart = declarationEnd
            while (declarationStart > 0 && isJavaScriptIdentifierCharacter(script[declarationStart - 1])) {
                declarationStart--
            }
            val declaration = script.substring(declarationStart, declarationEnd)
            val hasDeclaration = declaration.equals("var", ignoreCase = true) ||
                    declaration.equals("let", ignoreCase = true) ||
                    declaration.equals("const", ignoreCase = true)
            val isStandalone = beforeVariable == 0 ||
                    (!isJavaScriptIdentifierCharacterBefore(
                        script,
                        beforeVariable
                    ) && script[beforeVariable - 1] != '.')
            if (hasWindowPrefix || hasDeclaration || isStandalone) {
                var equalsIndex = variableEnd
                while (equalsIndex < script.length && script[equalsIndex].isWhitespace()) equalsIndex++
                if (equalsIndex < script.length && script[equalsIndex] == '=') return equalsIndex + 1
            }
        }
        searchFrom = variableStart + 1
    }
    return null
}
