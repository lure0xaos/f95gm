package f95gm.client.ui.shared.pagination

internal value class PageNumber(val value: Int) {
    init { require(value > 0) { "Page number must be positive" } }
}
