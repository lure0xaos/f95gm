package f95gm.domain.api

import kotlin.jvm.JvmInline

@JvmInline
value class ApiPath(val value: String)

@JvmInline
value class ApiQueryParameter(val value: String)

@JvmInline
value class ApiPathParameter(val value: String)

@JvmInline
value class ApiVersion(val value: String) {
    init { require(value.isNotBlank()) { "API version must not be blank" } }
}

@JvmInline
value class ApiSunsetDate(val value: String) {
    init {
        require(isCalendarDate(value)) { "API deprecation sunset must be a valid yyyy-MM-dd date" }
    }
}

private fun isCalendarDate(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    val year = value.substring(0, 4).toInt()
    val month = value.substring(5, 7).toInt()
    val day = value.substring(8, 10).toInt()
    if (month !in 1..12) return false
    val leapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val daysInMonth = when (month) {
        2 -> if (leapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return day in 1..daysInMonth
}

data class ApiDeprecationMetadata(
    val replacement: ApiPath,
    val sunset: ApiSunsetDate
)

internal fun apiPath(value: String): ApiPath = ApiPath(value)
internal fun apiQueryParameter(value: String): ApiQueryParameter = ApiQueryParameter(value)
