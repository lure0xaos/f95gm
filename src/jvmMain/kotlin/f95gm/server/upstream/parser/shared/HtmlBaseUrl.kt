package f95gm.server.upstream.parser.shared

@JvmInline
internal value class HtmlBaseUrl(val value: String) {
    init { require(value.isNotBlank()) }
}
