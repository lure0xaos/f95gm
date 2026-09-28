package f95gm.client.state.dropdown

import f95gm.domain.values.F95OptionId
internal value class DropdownSearchKey(val value: String)

internal object DropdownSearchKeys {
    val SEARCH_IN = DropdownSearchKey("searchIn")
    val DATE_LIMIT = DropdownSearchKey("dateLimit")
    val TAG_MATCHING = DropdownSearchKey("tagMatching")
    val INCLUDE_TAGS = DropdownSearchKey("includeTags")
    val EXCLUDE_TAGS = DropdownSearchKey("excludeTags")
    fun includePrefix(id: F95OptionId) = DropdownSearchKey("includePrefix:${id.value}")
    fun excludePrefix(id: F95OptionId) = DropdownSearchKey("excludePrefix:${id.value}")
}
