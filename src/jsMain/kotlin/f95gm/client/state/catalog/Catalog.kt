package f95gm.client.state.catalog

import dev.fritz2.core.RootStore
import f95gm.client.model.catalog.CatalogState
import kotlinx.coroutines.Job

internal val catalog = RootStore(CatalogState(), Job())
