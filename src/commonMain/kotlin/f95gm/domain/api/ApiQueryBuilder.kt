package f95gm.domain.api

import f95gm.domain.http.percentEncode

/** Builds query strings from contract-owned parameter names and encoded values. */
class ApiQueryBuilder internal constructor(private val base: String) {
    private val values = mutableListOf<Pair<ApiQueryParameter, String>>()

    fun add(parameter: ApiQueryParameter, value: String): ApiQueryBuilder {
        values += parameter to value
        return this
    }

    fun build(): ApiPath = ApiPath(buildString {
        append(base)
        values.forEachIndexed { index, (parameter, value) ->
            append(if (index == 0) '?' else '&')
            append(parameter.value).append('=').append(percentEncode(value))
        }
    })
}

fun apiQuery(base: String): ApiQueryBuilder = ApiQueryBuilder(base)
