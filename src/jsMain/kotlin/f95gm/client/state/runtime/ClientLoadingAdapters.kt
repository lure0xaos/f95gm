package f95gm.client.state.runtime

import f95gm.domain.filters.F95Category
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95ThreadId

internal open class ClientFeatureLoadingAdapter(
    private val controller: ClientController,
    private val feature: ClientLoadState
) : ClientFeatureLoadingPort {
    override fun isLoaded(): Boolean = controller.isLoaded(feature)
    override fun markLoaded() = controller.markLoaded(feature)
    override fun clearLoaded() = controller.clearLoaded(feature)
}

internal class ClientCatalogLoadingAdapter(private val controller: ClientController) :
    ClientFeatureLoadingAdapter(controller, ClientLoadState.CATALOG), ClientCatalogLoadingPort {
    override var loadedOptionsCategory: F95Category?
        get() = controller.loadedOptionsCategory
        set(value) { controller.loadedOptionsCategory = value }
}

internal class ClientFiltersLoadingAdapter(private val controller: ClientController) :
    ClientFeatureLoadingAdapter(controller, ClientLoadState.SAVED_FILTERS), ClientFiltersLoadingPort {
    override var appliedFilterId: F95FilterId?
        get() = controller.appliedFilterId
        set(value) { controller.appliedFilterId = value }
}

internal class ClientDetailLoadingAdapter(private val controller: ClientController) : ClientDetailLoadingPort {
    override var loadedDetailId: F95ThreadId?
        get() = controller.loadedDetailId
        set(value) { controller.loadedDetailId = value }
}
