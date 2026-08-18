package f95gm.client.state.dropdown

import dev.fritz2.core.RootStore
import kotlinx.coroutines.Job

internal val dropdownSearches = RootStore<Map<String, String>>(emptyMap(), Job())
