package f95gm.client.ui.styles

internal class CssBuilder {
    private val rules = mutableListOf<String>()

    operator fun String.invoke(block: CssRuleBuilder.() -> Unit) {
        val selector = this
        rules += CssRuleBuilder().apply(block).render(selector)
    }

    fun build(): List<String> = rules.toList()
}
