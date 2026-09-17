package f95gm.client.validation

import f95gm.client.model.settings.PageSize
import f95gm.domain.defaults.F95Defaults
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

import f95gm.domain.api.parseIntOrNull

internal object ClientInputValidation {
    fun pageSize(raw: String): PageSize = PageSize.fromValue(raw.parseIntOrNull())

    fun dateLimit(raw: String): Int = raw.parseIntOrNull()
        ?.takeIf { it >= F95Defaults.ANY_DATE }
        ?: F95Defaults.ANY_DATE

    fun filterName(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() }

    fun threadId(raw: String): F95ThreadId? = raw.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.let(::F95ThreadId)

    fun filterId(raw: String): F95FilterId? = raw.trim().takeIf { it.isNotEmpty() }?.let(::F95FilterId)
}
