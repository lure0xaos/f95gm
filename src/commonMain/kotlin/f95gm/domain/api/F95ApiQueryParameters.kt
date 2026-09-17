package f95gm.domain.api

/** Canonical query keys shared by browser requests and server-side contract metadata. */
object F95ApiQueryParameters {
    val PROBE = ApiQueryParameter("probe")
    val TAB_ID = ApiQueryParameter("tabId")
    val THEME = ApiQueryParameter("theme")
    val CATEGORY = ApiQueryParameter("category")
    val PAGE = ApiQueryParameter("page")
    val ROWS = ApiQueryParameter("rows")
    val SORT = ApiQueryParameter("sort")
    val SEARCH = ApiQueryParameter("search")
    val CREATOR = ApiQueryParameter("creator")
    val DATE = ApiQueryParameter("date")
    val TAG_TYPE = ApiQueryParameter("tagtype")
    val TAGS = ApiQueryParameter("tags")
    val EXCLUDED_TAGS = ApiQueryParameter("notags")
    val PREFIXES = ApiQueryParameter("prefixes")
    val EXCLUDED_PREFIXES = ApiQueryParameter("noprefixes")
    val URL = ApiQueryParameter("url")
    val COMMAND = ApiQueryParameter("cmd")
    val UPSTREAM_CATEGORY = ApiQueryParameter("cat")
    val CACHE_BUSTER = ApiQueryParameter("_")
}
