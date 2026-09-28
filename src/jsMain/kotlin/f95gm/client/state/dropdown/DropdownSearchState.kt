package f95gm.client.state.dropdown

internal data class DropdownSearchState(
    val values: Map<DropdownSearchKey, String> = emptyMap()
) {
    operator fun get(key: DropdownSearchKey): String = values[key].orEmpty()
    fun update(key: DropdownSearchKey, value: String): DropdownSearchState =
        copy(values = values + (key to value))
}
