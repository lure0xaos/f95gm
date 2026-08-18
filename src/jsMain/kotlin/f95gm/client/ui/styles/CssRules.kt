package f95gm.client.ui.styles

internal fun cssRules(block: CssBuilder.() -> Unit): List<String> =
    CssBuilder().apply(block).build()
