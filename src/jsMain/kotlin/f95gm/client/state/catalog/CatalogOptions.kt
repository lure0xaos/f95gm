package f95gm.client.state.catalog

import dev.fritz2.core.RootStore
import f95gm.client.model.catalog.CatalogOptions
import kotlinx.coroutines.Job

internal val catalogOptions = RootStore(CatalogOptions(), Job())
