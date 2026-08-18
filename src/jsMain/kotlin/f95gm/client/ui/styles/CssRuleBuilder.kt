package f95gm.client.ui.styles

internal class CssRuleBuilder {
    private val declarations = mutableListOf<String>()

    operator fun String.invoke(value: String) {
        declarations += "$this: $value"
    }

    fun render(selector: String): String = buildString {
        append(selector)
        append(" { ")
        append(declarations.joinToString("; "))
        append("; }")
    }
}
