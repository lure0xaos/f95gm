package f95gm.client.model.runtime

import f95gm.domain.api.parseIntOrNull

internal value class PageInput(val value: String) {
    fun toIntOrNull(): Int? = value.parseIntOrNull()
    override fun toString(): String = value
    companion object { val EMPTY = PageInput("") }
}

internal value class MarkSearchQuery(val value: String) {
    fun trim(): String = value.trim()
    fun isBlank(): Boolean = value.isBlank()
    override fun toString(): String = value
    companion object { val EMPTY = MarkSearchQuery("") }
}

internal value class CatalogSearchQuery(val value: String) {
    fun isNotBlank(): Boolean = value.isNotBlank()
    override fun toString(): String = value
    companion object { val EMPTY = CatalogSearchQuery("") }
}

internal value class DateLimit(val value: Int) {
    operator fun compareTo(other: Int): Int = value.compareTo(other)
    override fun toString(): String = value.toString()
    companion object { val ANY = DateLimit(-1) }
}

internal value class EndpointId(val value: String)

internal value class PayloadField(val value: String)
