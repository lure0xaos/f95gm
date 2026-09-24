package f95gm.client.state.filters

import f95gm.client.state.runtime.clientController

internal var loadedSavedFilters
    get() = clientController.savedFiltersLoaded
    set(value) {
        clientController.savedFiltersLoaded = value
    }
